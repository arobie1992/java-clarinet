package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.Handler;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private SimpleNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.peerStore = Objects.requireNonNull(builder.peerStore);
        this.transport = new TransportProxy(Objects.requireNonNull(builder.transportFactory.get()));
        this.transport.addInternal(Endpoints.CONNECT.name(), new ConnectHandlerProxy(builder.connectHandler, connectionStore, this));
        this.transport.addInternal(Endpoints.WITNESS.name(), new WitnessHandlerProxy(builder.witnessHandler, connectionStore, this));
        this.trustFilter = Objects.requireNonNull(builder.trustFilter);
        this.reputationStore = Objects.requireNonNull(builder.reputationStore);
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
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        return connectionId;
    }

    @Override
    public void addConnectHandler(Handler<ConnectRequest, ConnectResponse> connectHandler) {
        this.transport.add(Endpoints.CONNECT.name(), new ConnectHandlerProxy(connectHandler, connectionStore, this));
    }

    @Override
    public void removeConnectHandler() {
        this.transport.add(Endpoints.CONNECT.name(), new ConnectHandlerProxy(null, connectionStore, this));
    }

    static class Builder implements NodeBuilder {
        private PeerId id;
        private PeerStore peerStore;
        private Supplier<Transport> transportFactory;
        private Handler<ConnectRequest, ConnectResponse> connectHandler;
        private Function<Stream<? extends Reputation>, Stream<PeerId>> trustFilter;
        private ReputationStore reputationStore;
        private Handler<WitnessRequest, WitnessResponse> witnessHandler;

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
        public NodeBuilder connectHandler(Handler<ConnectRequest, ConnectResponse> connectHandler) {
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
        public NodeBuilder witnessHandler(Handler<WitnessRequest, WitnessResponse> witnessHandler) {
            this.witnessHandler = witnessHandler;
            return this;
        }

        @Override
        public Node build() {
            return new SimpleNode(this);
        }
    }

}
