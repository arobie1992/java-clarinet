package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.crypto.SigningException;
import com.github.arobie1992.clarinet.impl.netty.ConnectionIdSerializer;
import com.github.arobie1992.clarinet.impl.netty.PeerIdSerializer;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.message.MessageStore;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

class SimpleNode implements Node {

    private static final Logger log = LoggerFactory.getLogger(SimpleNode.class);

    private final PeerId id;
    private final PeerStore peerStore;
    private final ConnectionStore connectionStore = new ConnectionStore();
    private final TransportProxy transport;
    private final Function<Stream<? extends Reputation>, Stream<PeerId>> trustFilter;
    private final ReputationStore reputationStore;
    private final MessageStore messageStore;
    private final KeyStore keyStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SimpleNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.peerStore = Objects.requireNonNull(builder.peerStore, "peerStore");

        this.transport = new TransportProxy(Objects.requireNonNull(builder.transportFactory.get()));
        this.transport.addInternal(Endpoints.CONNECT.name(), new ConnectHandlerProxy(builder.connectHandler, connectionStore, this));
        this.transport.addInternal(Endpoints.WITNESS.name(), new WitnessHandlerProxy(builder.witnessHandler, connectionStore, this));
        this.transport.addInternal(
                Endpoints.WITNESS_NOTIFICATION.name(),
                new WitnessNotificationHandlerProxy(builder.witnessNotificationHandler, connectionStore, this)
        );
        this.transport.addInternal(Endpoints.MESSAGE.name(), new MessageHandlerProxy(builder.messageHandler, connectionStore, this));
        this.transport.addInternal(Endpoints.REQUEST_PEERS.name(), new PeersRequestHandlerProxy(builder.peersRequestHandler, this));
        this.transport.addInternal(Endpoints.REQUEST_KEYS.name(), new KeysRequestHandlerProxy(builder.keysRequestHandler, this));

        this.trustFilter = Objects.requireNonNull(builder.trustFilter, "trustFilter");
        this.reputationStore = Objects.requireNonNull(builder.reputationStore, "reputationStore");
        this.messageStore = Objects.requireNonNull(builder.messageStore, "messageStore");
        this.keyStore = Objects.requireNonNull(builder.keyStore, "keyStore");

        var module = new SimpleModule();
        module.addSerializer(PeerId.class, new PeerIdSerializer());
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());
    }

    @Override
    public PeerId id() {
        return id;
    }

    @Override
    public PeerStore peerStore() {
        return peerStore;
    }

    @Override
    public Transport transport() {
        return transport;
    }

    @Override
    public Connection.ReadableReference findConnection(ConnectionId connectionId) {
        return connectionStore.findForRead(connectionId);
    }

    @Override
    public MessageStore messageStore() {
        return messageStore;
    }

    @Override
    public KeyStore keyStore() {
        return keyStore;
    }

    private <T> Stream<T> exchangeForPeer(
            Peer peer,
            String endpoint,
            Object request,
            Class<T> responseType,
            TransportOptions transportOptions
    ) {
        return peer.addresses().stream().map(addr -> {
            try {
                return transport.exchange(addr, endpoint, request, responseType, transportOptions);
            } catch(RuntimeException e) {
                // TODO decide if it's worth signaling this back to the caller
                // I think I'm going to do this through an error handler
                // input will be the address and the exception
                log.warn("Encountered error while sending exchange to peer {} at address {} for endpoint {}", peer, addr, endpoint, e);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    private void sendForPeer(Peer peer, String endpoint, Object request, TransportOptions transportOptions) {
        for(var addr : peer.addresses()) {
            try {
                transport.send(addr, endpoint, request, transportOptions);
                // return for the first address that is successful
                return;
            } catch(RuntimeException e) {
                // TODO decide if it's worth signaling this back to the caller
                // I think I'm going to do this through an error handler
                // input will be the address and the exception
                log.warn("Encountered error while sending to peer {} at address {} for endpoint {}", peer, addr, endpoint, e);
            }
        }
        throw new PeerSendException(peer.id());
    }

    void sendInternal(PeerId peerId, DataMessage message, TransportOptions transportOptions) {
        var peer = peerStore().find(peerId).orElseThrow(() -> new NoSuchPeerException(peerId));
        sendForPeer(peer, Endpoints.MESSAGE.name(), message, transportOptions);
    }

    @Override
    public ConnectionId connect(PeerId receiver, ConnectionOptions connectionOptions, TransportOptions transportOptions) {
        var connectionId = connectionStore.create(id(), receiver, Connection.Status.REQUESTING_RECEIVER);
        var peer = peerStore.find(receiver).orElseThrow(() -> new NoSuchPeerException(receiver));
        ConnectResponse connectResponse = exchangeForPeer(
                peer,
                Endpoints.CONNECT.name(),
                new ConnectRequest(connectionId, id()),
                ConnectResponse.class,
                transportOptions
        ).findFirst().orElseThrow(ConnectFailureException::new);

        if(connectResponse.rejected()) {
            throw new ConnectRejectedException(connectResponse.reason());
        }

        record PeerAndResponse(Peer peer, WitnessResponse witnessResponse) {}
        Predicate<PeerAndResponse> byRejected = par -> {
            if(par.witnessResponse.rejected()) {
                // see about adding a handler here as well
                log.info("Witness {} rejected due to reason: {}", par.peer.id(), par.witnessResponse.reason());
                return false;
            } else {
                return true;
            }
        };

        var witness = trustFilter.apply(reputationStore.findAll(
                // filter out this node itself and the receiver
                peerStore.all().filter(pid -> !id.equals(pid) && !receiver.equals(pid)))
                ).map(peerStore::find)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(p -> exchangeForPeer(
                        p, Endpoints.WITNESS.name(), new WitnessRequest(connectionId, id(), receiver), WitnessResponse.class, transportOptions)
                        .map(wr -> new PeerAndResponse(p, wr)))
                .flatMap(r -> r.filter(par -> par.witnessResponse != null))
                .filter(byRejected)
                .map(par -> par.peer)
                .findFirst()
                .orElseThrow(WitnessSelectionException::new);

        try(var ref = connectionStore.findForWrite(connectionId)) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new RuntimeException("Connect attempt failed");
            }
            connection.setWitness(witness.id());
            connection.setStatus(Connection.Status.NOTIFYING_OF_WITNESS);
        }

        sendForPeer(peer, Endpoints.WITNESS_NOTIFICATION.name(), new WitnessNotification(connectionId, witness.id()), transportOptions);
        /*
         sendForPeer only returns if the send was successful as near as this side can tell, so will only reach this part
         if it seems successful.
         */
        try(var ref = connectionStore.findForWrite(connectionId)) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new NoSuchConnectionException(connectionId);
            }
            connection.setStatus(Connection.Status.OPEN);
        }

        return connectionId;
    }

    byte[] genSignature(Object parts) {
        byte[] serialized;
        try {
            serialized = objectMapper.writeValueAsBytes(parts);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        // java closure rules :\
        final byte[] finalSerialized = serialized;
        return keyStore.findPrivateKeys(id).stream().map(k -> {
            try {
                return k.sign(finalSerialized);
            } catch(SigningException e) {
                log.info("Encountered error while attempting to sign", e);
                // TODO error handler here too
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElseThrow(() -> new SigningException("Failed to sign message"));
    }

    @Override
    public MessageId send(ConnectionId connectionId, byte[] data, TransportOptions transportOptions) {
        try(var ref = connectionStore.findForWrite(connectionId)) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new NoSuchConnectionException(connectionId);
            }
            if(!connection.status().equals(Connection.Status.OPEN)) {
                throw new ConnectionStatusException(connectionId, "send", connection.status(), List.of(Connection.Status.OPEN));
            }
            var messageId = new MessageId(connectionId, connection.nextSequenceNumber());
            var message = new DataMessage(messageId, data);
            message.setSenderSignature(genSignature(message.senderParts()));
            messageStore.add(message);
            sendInternal(connection.witness().orElseThrow(), message, transportOptions);
            return message.messageId();
        }
    }

    @Override
    public PeersResponse requestPeers(PeerId requestee, PeersRequest request, TransportOptions transportOptions) {
        var peer = peerStore.find(requestee).orElseThrow(() -> new NoSuchPeerException(requestee));
        return exchangeForPeer(peer, Endpoints.REQUEST_PEERS.name(), request, PeersResponse.class, transportOptions)
                .findFirst()
                .orElseThrow(() -> new PeersRequestException(requestee));
    }

    @Override
    public KeysResponse requestKeys(PeerId requestee, KeysRequest request, TransportOptions transportOptions) {
        var peer = peerStore.find(requestee).orElseThrow(() -> new NoSuchPeerException(requestee));
        return exchangeForPeer(peer, Endpoints.REQUEST_KEYS.name(), request, KeysResponse.class, transportOptions)
                .findFirst()
                .orElseThrow(() -> new KeysRequestException(requestee));
    }

    @Override
    public void addConnectHandler(ExchangeHandler<ConnectRequest, ConnectResponse> connectHandler) {
        this.transport.addInternal(Endpoints.CONNECT.name(), new ConnectHandlerProxy(connectHandler, connectionStore, this));
    }

    @Override
    public void removeConnectHandler() {
        this.transport.addInternal(Endpoints.CONNECT.name(), new ConnectHandlerProxy(null, connectionStore, this));
    }

    @Override
    public void addWitnessHandler(ExchangeHandler<WitnessRequest, WitnessResponse> witnessHandler) {
        this.transport.addInternal(Endpoints.WITNESS.name(), new WitnessHandlerProxy(witnessHandler, connectionStore, this));
    }

    @Override
    public void removeWitnessHandler() {
        this.transport.addInternal(Endpoints.WITNESS.name(), new WitnessHandlerProxy(null, connectionStore, this));
    }

    @Override
    public void addWitnessNotificationHandler(SendHandler<WitnessNotification> witnessNotificationHandler) {
        this.transport.addInternal(
                Endpoints.WITNESS_NOTIFICATION.name(),
                new WitnessNotificationHandlerProxy(witnessNotificationHandler, connectionStore, this)
        );
    }

    @Override
    public void removeWitnessNotificationHandler() {
        this.transport.addInternal(
                Endpoints.WITNESS_NOTIFICATION.name(),
                new WitnessNotificationHandlerProxy(null, connectionStore, this)
        );
    }

    @Override
    public void addPeersRequestHandler(ExchangeHandler<PeersRequest, PeersResponse> peersRequestHandler) {
        this.transport.addInternal(Endpoints.REQUEST_PEERS.name(), new PeersRequestHandlerProxy(peersRequestHandler, this));
    }

    @Override
    public void removePeersRequestHandler() {
        this.transport.addInternal(Endpoints.REQUEST_PEERS.name(), new PeersRequestHandlerProxy(null, this));
    }

    @Override
    public void addMessageHandler(SendHandler<DataMessage> messageHandler) {
        this.transport.addInternal(Endpoints.MESSAGE.name(), new MessageHandlerProxy(messageHandler, connectionStore, this));
    }

    @Override
    public void removeMessageHandler() {
        this.transport.addInternal(Endpoints.MESSAGE.name(), new MessageHandlerProxy(null, connectionStore, this));
    }

    @Override
    public void addKeysRequestHandler(ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler) {
        this.transport.addInternal(Endpoints.REQUEST_KEYS.name(), new KeysRequestHandlerProxy(keysRequestHandler, this));
    }

    @Override
    public void removeKeysRequestHandler() {
        this.transport.addInternal(Endpoints.REQUEST_KEYS.name(), new KeysRequestHandlerProxy(null, this));
    }

    static class Builder implements NodeBuilder {
        private PeerId id;
        private PeerStore peerStore;
        private Supplier<Transport> transportFactory;
        private ExchangeHandler<ConnectRequest, ConnectResponse> connectHandler;
        private Function<Stream<? extends Reputation>, Stream<PeerId>> trustFilter;
        private ReputationStore reputationStore;
        private ExchangeHandler<WitnessRequest, WitnessResponse> witnessHandler;
        private SendHandler<WitnessNotification> witnessNotificationHandler;
        private MessageStore messageStore;
        private KeyStore keyStore;
        private SendHandler<DataMessage> messageHandler;
        private ExchangeHandler<PeersRequest, PeersResponse> peersRequestHandler;
        private ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler;

        @Override
        public NodeBuilder id(PeerId id) {
            this.id = id;
            return this;
        }

        @Override
        public NodeBuilder peerStore(PeerStore peerStore) {
            this.peerStore = peerStore;
            return this;
        }

        @Override
        public NodeBuilder transport(Supplier<Transport> transportFactory) {
            this.transportFactory = transportFactory;
            return this;
        }

        @Override
        public NodeBuilder connectHandler(ExchangeHandler<ConnectRequest, ConnectResponse> connectHandler) {
            this.connectHandler = connectHandler;
            return this;
        }

        @Override
        public NodeBuilder trustFilter(Function<Stream<? extends Reputation>, Stream<PeerId>> trustFunction) {
            this.trustFilter = trustFunction;
            return this;
        }

        @Override
        public NodeBuilder reputationStore(ReputationStore reputationStore) {
            this.reputationStore = reputationStore;
            return this;
        }

        @Override
        public NodeBuilder witnessHandler(ExchangeHandler<WitnessRequest, WitnessResponse> witnessHandler) {
            this.witnessHandler = witnessHandler;
            return this;
        }

        @Override
        public NodeBuilder witnessNotificationHandler(SendHandler<WitnessNotification> witnessNotificationHandler) {
            this.witnessNotificationHandler = witnessNotificationHandler;
            return this;
        }

        @Override
        public NodeBuilder messageStore(MessageStore messageStore) {
            this.messageStore = messageStore;
            return this;
        }

        @Override
        public NodeBuilder keyStore(KeyStore keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        @Override
        public NodeBuilder messageHandler(SendHandler<DataMessage> messageHandler) {
            this.messageHandler = messageHandler;
            return this;
        }

        @Override
        public NodeBuilder peersRequestHandler(ExchangeHandler<PeersRequest, PeersResponse> peersRequestHandler) {
            this.peersRequestHandler = peersRequestHandler;
            return this;
        }

        @Override
        public NodeBuilder keysRequestHandler(ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler) {
            this.keysRequestHandler = keysRequestHandler;
            return this;
        }

        @Override
        public Node build() {
            return new SimpleNode(this);
        }
    }

}
