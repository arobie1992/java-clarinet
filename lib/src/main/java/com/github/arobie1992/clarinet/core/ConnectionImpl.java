package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A logical connection in the Clarinet protocol.
 * <p>
 * Connections consist of a sender, a witness, and a receiver. The sender and receiver are fixed upon creation of the
 * connection. Witnesses must be requested contingent upon the sender and receiver so cannot be fixed at time of creation.
 * Once a witness is set, it cannot be updated.
 */
non-sealed class ConnectionImpl implements Connection {

    private final ConnectionId id;
    private final PeerId sender;
    private PeerId witness;
    private final PeerId receiver;
    private Status status;
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private long nextSeqNo = 0;

    ConnectionImpl(ConnectionId id, PeerId sender, PeerId receiver, Status status) {
        this.id = Objects.requireNonNull(id);
        this.sender = Objects.requireNonNull(sender);
        this.receiver = Objects.requireNonNull(receiver);
        this.status = Objects.requireNonNull(status);
    }

    @Override
    public ConnectionId id() {
        return id;
    }

    @Override
    public PeerId sender() {
        return sender;
    }

    void setWitness(PeerId witness) {
        assertWriteLocked();
        if(this.witness != null) {
            throw new UnsupportedOperationException("Cannot update witness once it has been set.");
        }
        this.witness = witness;
    }

    @Override
    public Optional<PeerId> witness() {
        return Optional.ofNullable(witness);
    }

    @Override
    public PeerId receiver() {
        return receiver;
    }

    void setStatus(Status status) {
        assertWriteLocked();
        this.status = status;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public long nextSequenceNumber() {
        assertWriteLocked();
        if(nextSeqNo < 0) {
            throw new ArithmeticException("nextSequenceNumber overflow");
        }
        var seqNo = nextSeqNo;
        nextSeqNo++;
        return seqNo;
    }

    @Override
    public List<PeerId> participants() {
        return witness == null ? List.of(sender, receiver) : List.of(sender, witness, receiver);
    }

    private void assertWriteLocked() {
        if(!lock.isWriteLocked()) {
            var operation = StackWalker.getInstance().walk(s -> s.skip(1).findFirst().orElseThrow().getMethodName());
            throw new WriteLockException(operation);
        }
    }
}
