package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.crypto.*;
import com.github.arobie1992.clarinet.impl.netty.ConnectionIdSerializer;
import com.github.arobie1992.clarinet.impl.netty.PeerIdSerializer;
import com.github.arobie1992.clarinet.message.*;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.reputation.AssessmentStore;
import com.github.arobie1992.clarinet.reputation.ReputationService;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

class SimpleNode implements Node {

    private static final Logger log = LoggerFactory.getLogger(SimpleNode.class);

    private final PeerId id;
    private final PeerStore peerStore;
    final ConnectionStore connectionStore = new ConnectionStore();
    final TransportProxy transport;
    private final BiFunction<Stream<? extends PeerId>, Function<PeerId, Double>, Stream<? extends PeerId>> trustFilter;
    private final AssessmentStore assessmentStore;
    private final ReputationService reputationService;
    private final MessageStore messageStore;
    private final KeyStore keyStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SendHandler<DataMessage> witnessHandler;
    private SendHandler<DataMessage> receiveHandler;

    SimpleNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.peerStore = Objects.requireNonNull(builder.peerStore);
        this.reputationService = Objects.requireNonNull(builder.reputationService);

        this.transport = new TransportProxy(Objects.requireNonNull(builder.transportFactory.get()));
        this.transport.addInternal(Endpoints.CONNECT.name(), new ConnectHandlerProxy(builder.connectHandler, connectionStore, this));
        this.transport.addInternal(
                Endpoints.WITNESS.name(),
                new WitnessRequestHandlerProxy(builder.witnessRequestHandler, connectionStore, this)
        );
        this.transport.addInternal(
                Endpoints.WITNESS_NOTIFICATION.name(),
                new WitnessNotificationHandlerProxy(builder.witnessNotificationHandler, connectionStore, this)
        );
        this.witnessHandler = builder.witnessHandler;
        this.receiveHandler = builder.receiveHandler;
        this.transport.addInternal(
                Endpoints.MESSAGE.name(),
                new MessageHandlerProxy(witnessHandler, receiveHandler, connectionStore, this)
        );
        this.transport.addInternal(Endpoints.REQUEST_PEERS.name(), new PeersRequestHandlerProxy(builder.peersRequestHandler, this));
        this.transport.addInternal(Endpoints.REQUEST_KEYS.name(), new KeysRequestHandlerProxy(builder.keysRequestHandler, this));
        this.transport.addInternal(Endpoints.QUERY.name(), new QueryHandlerProxy(builder.queryHandler, this));
        this.transport.addInternal(Endpoints.CLOSE.name(), new CloseHandlerProxy(builder.closeHandler, connectionStore));
        this.transport.addInternal(
                Endpoints.MESSAGE_FORWARD.name(),
                new MessageForwardHandlerProxy(builder.messageForwardHandler, connectionStore, this)
        );
        this.transport.addInternal(
                Endpoints.QUERY_FORWARD.name(),
                new QueryForwardHandlerProxy(builder.queryForwardHandler, connectionStore, this)
        );

        this.trustFilter = Objects.requireNonNull(builder.trustFilter);
        this.assessmentStore = Objects.requireNonNull(builder.assessmentStore);
        this.messageStore = Objects.requireNonNull(builder.messageStore);
        this.keyStore = Objects.requireNonNull(builder.keyStore);

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
    public AssessmentStore assessmentStore() {
        return assessmentStore;
    }

    @Override
    public ReputationService reputationService() {
        return reputationService;
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

    void sendForPeer(Peer peer, String endpoint, Object request, TransportOptions transportOptions) {
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
        // have to do everything inside the write lock to prevent other threads modifying connection while connect operations are occurring.
        try(var ref = connectionStore.create(id(), receiver, Connection.Status.REQUESTING_RECEIVER)) {
            if (!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new RuntimeException("Failed to creation connection");
            }
            var connectionId = connection.id();

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

            // filter out this node itself and the receiver
            var witness = trustFilter.apply(
                            peerStore.all().filter(pid -> !id.equals(pid) && !receiver.equals(pid)),
                            reputationService::get
                    ).map(peerStore::find)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(p -> exchangeForPeer(
                            p,
                            Endpoints.WITNESS.name(),
                            new WitnessRequest(connectionId, id(), receiver), WitnessResponse.class, transportOptions
                    ).map(wr -> new PeerAndResponse(p, wr)))
                    .flatMap(r -> r.filter(par -> par.witnessResponse != null))
                    .filter(byRejected)
                    .map(par -> par.peer)
                    .findFirst()
                    .orElseThrow(WitnessSelectionException::new);

            connection.setWitness(witness.id());
            connection.setStatus(Connection.Status.NOTIFYING_OF_WITNESS);

            sendForPeer(peer, Endpoints.WITNESS_NOTIFICATION.name(), new WitnessNotification(connectionId, witness.id()), transportOptions);
            /*
             sendForPeer only returns if the send was successful as near as this side can tell, so will only reach this part
             if it seems successful.
             */
            connection.setStatus(Connection.Status.OPEN);
            return connectionId;
        }
    }

    Bytes genSignature(Object parts) {
        Bytes serialized;
        try {
            serialized = Bytes.of(objectMapper.writeValueAsBytes(parts));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        return genSignature(serialized);
    }

    Bytes genSignature(final Bytes data) {
        return keyStore.findPrivateKeys(id).stream().map(k -> {
            try {
                return k.sign(data);
            } catch(SigningException e) {
                log.info("Encountered error while attempting to sign", e);
                // TODO error handler here too
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElseThrow(() -> new SigningException("Failed to sign message"));
    }

    @Override
    public MessageId send(ConnectionId connectionId, Bytes data, TransportOptions transportOptions) {
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
    public void close(ConnectionId connectionId, CloseOptions closeOptions, TransportOptions transportOptions) {
        try(var ref = connectionStore.findForWrite(connectionId, closeOptions.connectionObtainTimeout().orElse(Duration.ofSeconds(10)))) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new NoSuchConnectionException(connectionId);
            }
            if(connection.status().equals(Connection.Status.CLOSED)) {
                return;
            }
            connection.setStatus(Connection.Status.CLOSING);

            connection.participants().stream()
                    .filter(p -> !p.equals(id))
                    .map(peerStore::find)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(p -> {
                        try {
                            sendForPeer(p, Endpoints.CLOSE.name(), new CloseRequest(connectionId), transportOptions);
                        } catch(RuntimeException e) {
                            log.warn("Encountered error while attempting to send close of connection {} to {}", connectionId, p, e);
                        }
                    });
            connection.setStatus(Connection.Status.CLOSED);
        }
    }

    @Override
    public QueryResult query(PeerId peerId, MessageId messageId, TransportOptions transportOptions) {
        var peer = peerStore.find(peerId).orElseThrow(() -> new NoSuchPeerException(peerId));
        var resp = exchangeForPeer(peer, Endpoints.QUERY.name(), new QueryRequest(messageId), QueryResponse.class, transportOptions)
                .findFirst().orElseThrow(QueryException::new);
        return new QueryResult(peerId, messageId, resp);
    }

    @Override
    public boolean processQueryResult(QueryResult queryResult, TransportOptions transportOptions) {
        var assessment = assessmentStore.find(queryResult.queriedPeer(), queryResult.queriedMessage());
        var resp = queryResult.queryResponse();
        List<PeerId> participants;
        try(var ref = connectionStore.findForRead(queryResult.queriedMessage().connectionId())) {
            if (!(ref instanceof Connection.Readable(Connection connection))) {
                throw new NoSuchConnectionException(queryResult.queriedMessage().connectionId());
            }
            participants = connection.participants();
        }
        var messageOpt = messageStore.find(queryResult.queriedMessage());

        var otherParticipant = getOtherParticipant(participants, queryResult.queriedPeer());
        boolean validSig = true;
        if(invalidSignature(resp, queryResult.queriedPeer())) {
            assessment = assessment.updateStatus(Assessment.Status.STRONG_PENALTY);
            validSig = false;
        } else if(!participants.contains(queryResult.queriedPeer())) {
            forward(otherParticipant, queryResult, transportOptions);
            return false;
        } else if(messageOpt.isEmpty()) {
            forward(otherParticipant, queryResult, transportOptions);
            return false;
        } else if(!responseMatches(messageOpt.get(), queryResult.queryResponse())) {
            if(directCommunication(queryResult.queriedPeer(), participants)) {
                assessment = assessment.updateStatus(Assessment.Status.STRONG_PENALTY);
            } else {
                assessment = assessment.updateStatus(Assessment.Status.WEAK_PENALTY);
                var otherAssessment = assessmentStore.find(otherParticipant, queryResult.queriedMessage());
                var otherUpdated = otherAssessment.updateStatus(Assessment.Status.WEAK_PENALTY);
                assessmentStore.save(otherUpdated, reputationService::update);
            }
        } else {
            assessment = assessment.updateStatus(Assessment.Status.REWARD);
        }
        assessmentStore.save(assessment, reputationService::update);
        if(validSig) {
            forward(otherParticipant, queryResult, transportOptions);
        }
        return true;
    }

    private void forward(PeerId peerId, QueryResult queryResult, TransportOptions transportOptions) {
        var sig = genSignature(queryResult.queryResponse());
        var forward = new QueryForward(queryResult.queriedPeer(), queryResult.queryResponse(), sig);
        var peer = peerStore.find(peerId).orElseThrow(() -> new NoSuchPeerException(peerId));
        sendForPeer(peer, Endpoints.QUERY_FORWARD.name(), forward, transportOptions);
    }

    private boolean invalidSignature(QueryResponse queryResponse, PeerId queriedPeer) {
        if (queryResponse.signature() == null) {
            return queryResponse.messageDetails().messageHash() != null;
        } else {
            try {
                return !checkSignature(queryResponse.messageDetails(), queriedPeer, queryResponse.signature());
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private boolean responseMatches(DataMessage message, QueryResponse queryResponse) {
        return Objects.equals(hash(message.witnessParts(), queryResponse.hashAlgorithm()), queryResponse.messageDetails().messageHash());
    }

    PeerId getOtherParticipant(List<PeerId> participants, PeerId queriedPeer) {
        var other = participants.stream()
                .filter(id -> !id.equals(id()))
                .filter(id -> !id.equals(queriedPeer))
                .toList();

        if(other.size() != 1) {
            var msg = "There were not exactly three participants in the connection including this node and the queried peer";
            throw new IllegalStateException(msg);
        }
        return other.getFirst();
    }

    boolean directCommunication(PeerId peerId, List<PeerId> participants) {
        var selfPos = participants.indexOf(id());
        if(selfPos == -1) {
            throw new UnsupportedOperationException("Node's id does not support equality checking or was not a participant in the connection");
        }
        var otherPos = participants.indexOf(peerId);
        if(otherPos == -1) {
            var msg = "Queried peer's id does not support equality checking or was not a participant in the connection";
            throw new UnsupportedOperationException(msg);
        }
        return Math.abs(selfPos - otherPos) == 1;
    }

    Bytes hash(Object data, String algorithm) {
        try {
            var enc = objectMapper.writeValueAsBytes(data);
            var digest = MessageDigest.getInstance(algorithm);
            return Bytes.of(digest.digest(enc));
        } catch (NoSuchAlgorithmException e) {
            throw new HashingException(e);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    boolean checkSignature(Bytes data, PeerId peerId, Bytes signature) {
        var keys = getOrLoadKeys(peerId);
        return  keys.stream()
                .map(k -> {
                    try {
                        return k.verify(data, signature);
                    } catch (RuntimeException e) {
                        log.debug("Encountered error for key {}", k, e);
                        return false;
                    }
                })
                .filter(v -> v)
                .findFirst()
                .orElse(false);
    }

    boolean checkSignatureHash(Bytes hash, PeerId peerId, Bytes signature) {
        var keys = getOrLoadKeys(peerId);
        return  keys.stream()
                .map(k -> {
                    try {
                        return k.verifyHash(hash, signature);
                    } catch (RuntimeException e) {
                        log.debug("Encountered error for key {}", k, e);
                        return false;
                    }
                })
                .filter(v -> v)
                .findFirst()
                .orElse(false);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    boolean checkSignature(Bytes data, PeerId peerId, Optional<Bytes> signature) {
        return signature.map(sig -> checkSignature(data, peerId, sig)).orElse(false);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    boolean checkSignature(Object parts, PeerId peerId, Optional<Bytes> signature) throws JsonProcessingException {
        var data = Bytes.of(objectMapper.writeValueAsBytes(parts));
        return checkSignature(data, peerId, signature);
    }

    boolean checkSignature(Object parts, PeerId peerId, Bytes signature) throws JsonProcessingException {
        var data = Bytes.of(objectMapper.writeValueAsBytes(parts));
        return checkSignature(data, peerId, signature);
    }

    private Collection<PublicKey> getOrLoadKeys(PeerId peerId) {
        var keys = keyStore().findPublicKeys(peerId);
        if(keys.isEmpty()) {
            keys = requestKeys(peerId, new KeysRequest(), new TransportOptions()).keys()
                    .stream()
                    .map(k -> keyStore().providers()
                            .filter(p -> p instanceof PublicKeyProvider)
                            .map(p -> (PublicKeyProvider) p)
                            .filter(p -> p.supports(k.algorithm()))
                            .map(p -> {
                                try {
                                    return p.create(k.bytes());
                                } catch (RuntimeException e) {
                                    log.debug("Encountered error creating key for RawKey {}", k.algorithm(), e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .findFirst())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            keys.forEach(k -> keyStore().addPublicKey(peerId, k));
        }
        return keys;
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
    public void addWitnessRequestHandler(ExchangeHandler<WitnessRequest, WitnessResponse> witnessRequestHandler) {
        this.transport.addInternal(Endpoints.WITNESS.name(), new WitnessRequestHandlerProxy(witnessRequestHandler, connectionStore, this));
    }

    @Override
    public void removeWitnessRequestHandler() {
        this.transport.addInternal(Endpoints.WITNESS.name(), new WitnessRequestHandlerProxy(null, connectionStore, this));
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
    public void addWitnessHandler(SendHandler<DataMessage> witnessHandler) {
        this.witnessHandler = witnessHandler;
        this.transport.addInternal(
                Endpoints.MESSAGE.name(),
                new MessageHandlerProxy(this.witnessHandler, receiveHandler, connectionStore, this)
        );
    }

    @Override
    public void removeWitnessHandler() {
        this.witnessHandler = null;
        this.transport.addInternal(
                Endpoints.MESSAGE.name(),
                new MessageHandlerProxy(null, receiveHandler, connectionStore, this)
        );
    }

    @Override
    public void addReceiveHandler(SendHandler<DataMessage> receiveHandler) {
        this.receiveHandler = receiveHandler;
        this.transport.addInternal(
                Endpoints.MESSAGE.name(),
                new MessageHandlerProxy(witnessHandler, this.receiveHandler, connectionStore, this)
        );
    }

    @Override
    public void removeReceiveHandler() {
        this.receiveHandler = null;
        this.transport.addInternal(
                Endpoints.MESSAGE.name(),
                new MessageHandlerProxy(witnessHandler, null, connectionStore, this)
        );
    }

    @Override
    public void addKeysRequestHandler(ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler) {
        this.transport.addInternal(Endpoints.REQUEST_KEYS.name(), new KeysRequestHandlerProxy(keysRequestHandler, this));
    }

    @Override
    public void removeKeysRequestHandler() {
        this.transport.addInternal(Endpoints.REQUEST_KEYS.name(), new KeysRequestHandlerProxy(null, this));
    }

    @Override
    public void addQueryHandler(ExchangeHandler<QueryRequest, QueryResponse> queryHandler) {
        this.transport.addInternal(Endpoints.QUERY.name(), new QueryHandlerProxy(queryHandler, this));
    }

    @Override
    public void removeQueryHandler() {
        this.transport.addInternal(Endpoints.QUERY.name(), new QueryHandlerProxy(null, this));
    }

    @Override
    public void addCloseHandler(SendHandler<CloseRequest> closeHandler) {
        this.transport.addInternal(Endpoints.CLOSE.name(), new CloseHandlerProxy(closeHandler, connectionStore));
    }

    @Override
    public void removeCloseHandler() {
        this.transport.addInternal(Endpoints.CLOSE.name(), new CloseHandlerProxy(null, connectionStore));
    }

    @Override
    public void addMessageForwardHandler(SendHandler<MessageForward> messageForwardHandler) {
        this.transport.addInternal(
                Endpoints.MESSAGE_FORWARD.name(),
                new MessageForwardHandlerProxy(messageForwardHandler, connectionStore, this)
        );
    }

    @Override
    public void removeMessageForwardHandler() {
        this.transport.addInternal(
                Endpoints.MESSAGE_FORWARD.name(),
                new MessageForwardHandlerProxy(null, connectionStore, this)
        );
    }

    static class Builder implements NodeBuilder {
        private PeerId id;
        private PeerStore peerStore;
        private Supplier<Transport> transportFactory;
        private ExchangeHandler<ConnectRequest, ConnectResponse> connectHandler;
        private BiFunction<Stream<? extends PeerId>, Function<PeerId, Double>, Stream<? extends PeerId>> trustFilter;
        private AssessmentStore assessmentStore;
        private ReputationService reputationService;
        private ExchangeHandler<WitnessRequest, WitnessResponse> witnessRequestHandler;
        private SendHandler<WitnessNotification> witnessNotificationHandler;
        private MessageStore messageStore;
        private KeyStore keyStore;
        SendHandler<DataMessage> witnessHandler;
        SendHandler<DataMessage> receiveHandler;
        private ExchangeHandler<PeersRequest, PeersResponse> peersRequestHandler;
        private ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler;
        private ExchangeHandler<QueryRequest, QueryResponse> queryHandler;
        private SendHandler<CloseRequest> closeHandler;
        private SendHandler<MessageForward> messageForwardHandler;
        private SendHandler<QueryForward> queryForwardHandler;

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
        public NodeBuilder trustFilter(BiFunction<Stream<? extends PeerId>, Function<PeerId, Double>, Stream<? extends PeerId>> trustFunction) {
            this.trustFilter = trustFunction;
            return this;
        }

        @Override
        public NodeBuilder assessmentStore(AssessmentStore assessmentStore) {
            this.assessmentStore = assessmentStore;
            return this;
        }

        @Override
        public NodeBuilder reputationService(ReputationService reputationService) {
            this.reputationService = reputationService;
            return this;
        }

        @Override
        public NodeBuilder witnessRequestHandler(ExchangeHandler<WitnessRequest, WitnessResponse> witnessRequestHandler) {
            this.witnessRequestHandler = witnessRequestHandler;
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
        public NodeBuilder witnessHandler(SendHandler<DataMessage> witnessHandler) {
            this.witnessHandler = witnessHandler;
            return this;
        }

        @Override
        public NodeBuilder receiveHandler(SendHandler<DataMessage> receiveHandler) {
            this.receiveHandler = receiveHandler;
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
        public NodeBuilder queryHandler(ExchangeHandler<QueryRequest, QueryResponse> queryHandler) {
            this.queryHandler = queryHandler;
            return this;
        }

        @Override
        public NodeBuilder closeHandler(SendHandler<CloseRequest> closeHandler) {
            this.closeHandler = closeHandler;
            return this;
        }

        @Override
        public NodeBuilder messageForwardHandler(SendHandler<MessageForward> messageForwardHandler) {
            this.messageForwardHandler = messageForwardHandler;
            return this;
        }

        @Override
        public NodeBuilder queryForwardHandler(SendHandler<QueryForward> queryForwardHandler) {
            this.queryForwardHandler = queryForwardHandler;
            return this;
        }

        @Override
        public Node build() {
            return new SimpleNode(this);
        }
    }

}
