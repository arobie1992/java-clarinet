package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.query.QueryTerms;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

class ConnectionStore {
    private final Map<ConnectionId, Connection> connections = new ConcurrentHashMap<>();

    WriteableReference create(PeerId sender, PeerId receiver, Connection.Status status) {
        var connectionId = ConnectionId.random();
        return accept(connectionId, sender, receiver, status);
    }

    WriteableReference accept(ConnectionId connectionId, PeerId sender, PeerId receiver, Connection.Status status) {
        var connection = connections.compute(connectionId, (id, existing) -> {
            if (existing != null) {
                throw new ExistingConnectionIdException(id);
            }
            var conn = new ConnectionImpl(connectionId, sender, receiver, status);
            conn.lock.writeLock().lock();
            return conn;
        });
        return new Writeable((ConnectionImpl) connection);
    }

    Connection.ReadableReference findForRead(ConnectionId connectionId) {
        return findForRead(connectionId, Duration.ofSeconds(10));
    }

    Connection.ReadableReference findForRead(ConnectionId connectionId, Duration timeout) {
        var connection = connections.get(Objects.requireNonNull(connectionId));
        if (connection == null) {
            return new Connection.Absent();
        }
        ConnectionImpl impl = (ConnectionImpl) connection;
        doLocking(connectionId, impl.lock.readLock(), timeout);
        return new Connection.Readable(connection);
    }

    WriteableReference findForWrite(ConnectionId connectionId) {
        return findForWrite(connectionId, Duration.ofSeconds(10));
    }

    WriteableReference findForWrite(ConnectionId connectionId, Duration timeout) {
        var connection = connections.get(Objects.requireNonNull(connectionId));
        if (connection == null) {
            return new Connection.Absent();
        }
        var impl = (ConnectionImpl) connection;
        doLocking(connectionId, impl.lock.writeLock(), timeout);
        return new Writeable(impl);
    }

    private void doLocking(ConnectionId connectionId, Lock lock, Duration timeout) {
        Objects.requireNonNull(timeout);
        try {
            if(!lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
                throw new ConnectionObtainException(connectionId, timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionObtainException(connectionId, e);
        }
    }

    Stream<Connection.Readable> query(QueryTerms<Connection> queryTerms) {
        return connections.values().stream()
                .sorted(queryTerms.order())
                .map(c -> {
                    var impl = (ConnectionImpl) c;
                    doLocking(impl.id(), impl.lock.readLock(), Duration.ofSeconds(10));
                    return new Connection.Readable(impl);
                })
                .filter(r -> {
                    var retain = queryTerms.where().test(r.connection());
                    if(!retain) {
                        ((ConnectionImpl) r.connection()).lock.readLock().unlock();
                    }
                    return retain;
                });
    }
}
