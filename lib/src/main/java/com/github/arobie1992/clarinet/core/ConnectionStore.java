package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// TODO decide on rollback behavior
class ConnectionStore {
    private final Map<ConnectionId, Connection> connections = new ConcurrentHashMap<>();

    ConnectionId create(PeerId sender, PeerId receiver, Connection.Status status) {
        var connectionId = ConnectionId.random();
        accept(connectionId, sender, receiver, status);
        return connectionId;
    }

    void accept(ConnectionId connectionId, PeerId sender, PeerId receiver, Connection.Status status) {
        connections.compute(connectionId, (id, existing) -> {
            if (existing != null) {
                throw new ExistingConnectionIdException(id);
            }
            return new ConnectionImpl(connectionId, sender, receiver, status);
        });
    }

    Connection.ReadableReference findForRead(ConnectionId connectionId) {
        var connection = connections.get(Objects.requireNonNull(connectionId));
        if (connection == null) {
            return new Connection.Absent();
        }
        ((ConnectionImpl) connection).lock.readLock().lock();
        return new Connection.Readable(connection);
    }

    WriteableReference findForWrite(ConnectionId connectionId) {
        var connection = connections.get(Objects.requireNonNull(connectionId));
        if (connection == null) {
            return new Connection.Absent();
        }
        var impl = (ConnectionImpl) connection;
        impl.lock.writeLock().lock();
        return new Writeable(impl);
    }
}
