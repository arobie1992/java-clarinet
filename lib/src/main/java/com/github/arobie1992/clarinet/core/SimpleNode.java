package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

class SimpleNode implements Node {

    private static final Logger log = LoggerFactory.getLogger(SimpleNode.class);

    private final PeerId id;
    private final PeerStore peerStore;
    private final ConnectionStore connectionStore = new ConnectionStore();
    private final Transport transport;

    private SimpleNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.peerStore = Objects.requireNonNull(builder.peerStore);
        this.transport = Objects.requireNonNull(builder.transport);
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
        ConnectResponse ignoredForNow = peer.addresses().stream().map(addr -> {
            try {
                return transport.exchange(addr, "connect", new ConnectRequest(), ConnectResponse.class, transportOptions);
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

    static class Builder implements NodeBuilder {
        private PeerId id;
        private PeerStore peerStore;
        private Transport transport;

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
        public NodeBuilder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public Node build() {
            return new SimpleNode(this);
        }
    }

}
