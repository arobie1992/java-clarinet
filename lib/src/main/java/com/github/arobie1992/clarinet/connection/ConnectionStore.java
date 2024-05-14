package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.Peer;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ConnectionStore {
    ConnectionId create(Peer sender, Peer receiver, ConnectionStatus status);
    void update(ConnectionId connectionId, Function<Connection, Connection> updateFunction);
    void read(ConnectionId connectionId, Consumer<Connection> readFunction);
    Collection<ConnectionId> all();
}
