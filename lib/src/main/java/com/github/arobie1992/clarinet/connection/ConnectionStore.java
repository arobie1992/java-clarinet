package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.Peer;

import java.util.function.Function;

public interface ConnectionStore {
    ConnectionId create(Peer sender, Peer receiver, ConnectionStatus status);
    void update(ConnectionId id, Function<Connection, Connection> updateFunction);
}
