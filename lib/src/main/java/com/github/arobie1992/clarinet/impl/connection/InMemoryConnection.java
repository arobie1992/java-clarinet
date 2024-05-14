package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.Connection;
import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionStatus;
import com.github.arobie1992.clarinet.peer.Peer;

import java.util.Optional;

public record InMemoryConnection(
        ConnectionId connectionId,
        ConnectionStatus status,
        Peer sender,
        Peer witnessPeer,
        Peer receiver
) implements Connection {
    @Override
    public Connection updateStatus(ConnectionStatus status) {
        return new InMemoryConnection(connectionId, status, sender, witnessPeer, receiver);
    }

    @Override
    public Optional<Peer> witness() {
        return Optional.ofNullable(witnessPeer);
    }

    @Override
    public Connection setWitness(Peer peer) {
        return new InMemoryConnection(connectionId, status, sender, peer, receiver);
    }
}
