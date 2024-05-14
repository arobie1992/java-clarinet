package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.*;
import com.github.arobie1992.clarinet.peer.Peer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class InMemoryConnectionStore implements ConnectionStore {

    private final Map<ConnectionId, Connection> connections = new ConcurrentHashMap<>();

    @Override
    public ConnectionId create(Peer sender, Peer receiver, ConnectionStatus status) {
        var connectionId = ConnectionId.random();
        connections.compute(connectionId, (id, existing) -> {
            if (existing != null) {
                throw new ExistingConnectionIDException(connectionId);
            }
            return new InMemoryConnection(id, status, sender, null, receiver);
        });
        return connectionId;
    }

    @Override
    public void update(ConnectionId connectionId, Function<Connection, Connection> updateFunction) {
        connections.compute(connectionId, (id, existing) -> {
            if(existing == null) {
                throw new NoSuchConnectionException(connectionId);
            }
            return updateFunction.apply(existing);
        });
    }

    @Override
    public Collection<ConnectionId> all() {
        return connections.keySet();
    }

    @Override
    public void read(ConnectionId connectionId, Consumer<Connection> readFunction) {
        connections.compute(connectionId, (id, val) -> {
            readFunction.accept(val == null ? null : ReadOnlyConnection.from(val));
            return val;
        });
    }
}
