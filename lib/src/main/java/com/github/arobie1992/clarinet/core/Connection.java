package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Optional;

public sealed interface Connection permits ConnectionImpl {
    ConnectionId id();
    PeerId sender();
    Optional<PeerId> witness();
    PeerId receiver();
    Connection.Status status();

    long nextSequenceNumber();

    /**
     * {@code Reference} allows access to a {@link Connection} and performs any necessary unlocking in its
     * {@link Reference#close()} method.
     */
    sealed interface Reference extends AutoCloseable permits ReadableReference, WriteableReference {
        // get rid of need to deal with checked exceptions since we don't potentially throw any
        @Override void close();
    }
    sealed interface ReadableReference extends Reference {}

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
