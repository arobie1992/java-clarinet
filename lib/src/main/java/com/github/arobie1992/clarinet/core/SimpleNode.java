package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.*;

class SimpleNode implements Node {

    private final PeerId id;
    private final PeerStore peerStore;
    private final ConnectionStore connectionStore = new ConnectionStore();
    private final Set<Address> addresses = new HashSet<>();

    private SimpleNode(Builder builder) {
        this.id = builder.id;
        this.peerStore = builder.peerStore;
    }

    @Override
    public PeerId id() {
        return id;
    }

    @Override
    public Set<Address> addresses() {
        return addresses;
    }

    @Override
    public PeerStore peerStore() {
        return peerStore;
    }

    @Override
    public Connection.ReadableReference findConnection(ConnectionId connectionId) {
        return connectionStore.findForRead(connectionId);
    }

    @Override
    public ConnectionId connect(PeerId receiver, ConnectionOptions connectionOptions, TransportOptions transportOptions) {
        var connectionId = connectionStore.create(id(), receiver, Connection.Status.REQUESTING_RECEIVER);
        var peer = peerStore.all().findFirst().orElseThrow();
        try(var ref = connectionStore.findForWrite(connectionId)) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new RuntimeException("Connect attempt failed");
            }
            connection.setWitness(peer.id());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return connectionId;
    }

    static class Builder implements NodeBuilder {
        private PeerId id;
        private PeerStore peerStore;

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
        public Node build() {
            return new SimpleNode(this);
        }
    }

}
