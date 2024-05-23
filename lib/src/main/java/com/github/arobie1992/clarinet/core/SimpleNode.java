package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.transport.Handler;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

class SimpleNode implements Node {

    private static final Logger log = LoggerFactory.getLogger(SimpleNode.class);

    private final PeerId id;
    private final PeerStore peerStore;
    private final ConnectionStore connectionStore = new ConnectionStore();
    private final TransportProxy transport;

    private SimpleNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.peerStore = Objects.requireNonNull(builder.peerStore);
        this.transport = new TransportProxy(Objects.requireNonNull(builder.transportFactory.get()));
        this.transport.addInternal(Endpoints.CONNECT.name(), new ConnectHandlerProxy(builder.connectHandler, connectionStore, this));
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
    public ConnectionId connect(PeerId receiver, ConnectionOptions connectionOptions, TransportOptions transportOptions) {
        var connectionId = connectionStore.create(id(), receiver, Connection.Status.REQUESTING_RECEIVER);
        var peer = peerStore.find(receiver).orElseThrow(() -> new NoSuchPeerException(receiver));
        ConnectResponse connectResponse = peer.addresses().stream().map(addr -> {
            try {
                var request = new ConnectRequest(connectionId, id());
                return transport.exchange(addr, Endpoints.CONNECT.name(), request, ConnectResponse.class, transportOptions);
            } catch(RuntimeException e) {
                // TODO decide if it's worth signaling this back to the caller
                // I think I'm going to do this through an error handler
                // input will be the address and the exception
                log.warn("Encountered error while trying to request connection to peer {} at address {}", receiver, addr, e);
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElseThrow(ConnectFailureException::new);

        var witness = peerStore.all().findFirst().orElseThrow();
        try(var ref = connectionStore.findForWrite(connectionId)) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new RuntimeException("Connect attempt failed");
            }
            connection.setWitness(witness.id());
            connection.setStatus(Connection.Status.OPEN);
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
        public Node build() {
            return new SimpleNode(this);
        }
    }

}
