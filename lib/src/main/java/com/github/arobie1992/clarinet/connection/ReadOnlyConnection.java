package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.Peer;

import java.util.Optional;

public record ReadOnlyConnection(ConnectionStatus status, Peer sender, Optional<Peer> witness, Peer receiver) implements Connection {
    @Override
    public Connection updateStatus(ConnectionStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection setWitness(Peer peer) {
        throw new UnsupportedOperationException();
    }

    public static ReadOnlyConnection from(Connection connection) {
        return new ReadOnlyConnection(connection.status(), connection.sender(), connection.witness(), connection.receiver());
    }
}
