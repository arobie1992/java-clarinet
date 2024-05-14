package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.connection.ConnectionStore;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.Transport;

import java.util.function.Predicate;

public class SimpleNodeBuilder implements NodeBuilder {
    ConnectionStore connectionStore;
    Transport transport;
    Predicate<Reputation> trustFunction;
    ReputationStore reputationStore;
    PeerStore peerStore;

    @Override
    public NodeBuilder trustFunction(Predicate<Reputation> trustFunction) {
        this.trustFunction = trustFunction;
        return this;
    }

    @Override
    public NodeBuilder transport(Transport transport) {
        this.transport = transport;
        return this;
    }

    @Override
    public NodeBuilder connectionStore(ConnectionStore connectionStore) {
        this.connectionStore = connectionStore;
        return this;
    }

    @Override
    public NodeBuilder reputationStore(ReputationStore reputationStore) {
        this.reputationStore = reputationStore;
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
