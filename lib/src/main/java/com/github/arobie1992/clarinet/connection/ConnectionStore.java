package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// TODO decide on rollback behavior
public class ConnectionStore {
    private final Map<ConnectionId, Connection> connections = new ConcurrentHashMap<>();

    public ConnectionId create(PeerId sender, PeerId receiver, Connection.Status status) {
        var connectionId = ConnectionId.random();
        connections.compute(connectionId, (id, existing) -> {
            if (existing != null) {
                throw new ExistingConnectionIdException(id);
            }
            return new ConnectionImpl(connectionId, sender, receiver, status);
        });
        return connectionId;
    }

    public ReadableReference findForRead(ConnectionId connectionId) {
        var connection = connections.get(Objects.requireNonNull(connectionId));
        if (connection == null) {
            return new Absent();
        }
        ((ConnectionImpl) connection).lock.readLock().lock();
        return new Readable(connection);
    }

    public WriteableReference findForWrite(ConnectionId connectionId) {
        var connection = connections.get(Objects.requireNonNull(connectionId));
        if (connection == null) {
            return new Absent();
        }
        ((ConnectionImpl) connection).lock.writeLock().lock();
        return new Writeable(connection);
    }

    /**
     * {@code ConnectionReference} allows access to a {@link Connection} and performs any necessary unlocking in its
     * {@link ConnectionReference#close()} method.
     */
    public sealed interface ConnectionReference extends AutoCloseable {}
    public sealed interface WriteableReference extends ConnectionReference {}
    public sealed interface ReadableReference extends ConnectionReference {}

    public record Writeable(Connection connection) implements WriteableReference {
        @Override
        public void close() {
            ((ConnectionImpl) connection).lock.writeLock().unlock();
        }
    }
    public record Readable(Connection connection) implements ReadableReference {
        @Override
        public void close() {
            ((ConnectionImpl) connection).lock.readLock().unlock();
        }
    }
    public record Absent() implements WriteableReference, ReadableReference {
        @Override
        public void close() {}
    }
}
