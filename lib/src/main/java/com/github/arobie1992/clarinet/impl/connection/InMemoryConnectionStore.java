package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.*;
import com.github.arobie1992.clarinet.impl.support.ReadWriteStore;
import com.github.arobie1992.clarinet.peer.Peer;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class InMemoryConnectionStore implements ConnectionStore {

    private final ReadWriteStore<ConnectionId, Connection> connections = new ReadWriteStore<>();

    @Override
    public ConnectionId create(Peer sender, Peer receiver, ConnectionStatus status) {
        var connectionId = ConnectionId.random();
        connections.write(connectionId, existing -> {
            if (existing != null) {
                throw new ExistingConnectionIdException(connectionId);
            }
            return new InMemoryConnection(connectionId, status, sender, null, receiver);
        });
        return connectionId;
    }

    @Override
    public void update(ConnectionId connectionId, Function<Connection, Connection> updateFunction) {
        connections.write(connectionId, existing -> {
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
        connections.read(connectionId, conn -> readFunction.accept(conn == null ? null : ReadOnlyConnection.from(conn)));
    }
}
