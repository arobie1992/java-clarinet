package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Optional;

public sealed interface Connection permits ConnectionImpl {
    ConnectionId id();
    PeerId sender();
    Optional<PeerId> witness();
    PeerId receiver();
    Connection.Status status();

    /**
     * {@code Reference} allows access to a {@link Connection} and performs any necessary unlocking in its
     * {@link Reference#close()} method.
     */
    sealed interface Reference extends AutoCloseable {}
    sealed interface WriteableReference extends Reference {}
    sealed interface ReadableReference extends Reference {}

    record Writeable(Connection connection) implements WriteableReference {
        @Override
        public void close() {
            ((ConnectionImpl) connection).lock.writeLock().unlock();
        }
    }
    record Readable(Connection connection) implements ReadableReference {
        @Override
        public void close() {
            ((ConnectionImpl) connection).lock.readLock().unlock();
        }
    }
    record Absent() implements WriteableReference, ReadableReference {
        @Override
        public void close() {}
    }

    enum Status {
        REQUESTING_RECEIVER,
        REQUESTING_SENDER,
        AWAITING_WITNESS,
        REQUESTING_WITNESS,
        NOTIFYING_OF_WITNESS,
        OPEN,
        CLOSING,
        CLOSED
    }
}
