package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.utils.PeerUtils;
import com.github.arobie1992.clarinet.utils.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionImplTest {

    private static final ConnectionId id = ConnectionId.random();
    private static final ConnectionImpl.Status status = ConnectionImpl.Status.REQUESTING_RECEIVER;

    private ConnectionImpl connection;

    @BeforeEach
    void setUp() {
        connection = new ConnectionImpl(id, PeerUtils.senderId(), PeerUtils.receiverId(), status);
    }

    @ParameterizedTest()
    @MethodSource("creationPermutations")
    void testCreation(ConnectionId id, PeerId sender, PeerId receiver, ConnectionImpl.Status status, TestConnection expected) {
        ThrowingSupplier<ConnectionImpl> create = () -> new ConnectionImpl(id, sender, receiver, status);
        if(expected == null) {
            assertThrows(NullPointerException.class, create::get);
        } else {
            expected.assertMatches(assertDoesNotThrow(create));
        }
    }

    @Test
    void testSetWitness() {
        connection.lock.writeLock().lock();
        assertDoesNotThrow(() -> connection.setWitness(PeerUtils.witnessId()));
        connection.lock.writeLock().unlock();
        assertEquals(Optional.of(PeerUtils.witnessId()), connection.witness());
    }

    @Test
    void testSetWitnessCannotUpdate() {
        connection.lock.writeLock().lock();
        connection.setWitness(PeerUtils.witnessId());
        var ex = assertThrows(UnsupportedOperationException.class, () -> connection.setWitness(PeerUtils.witnessId()));
        assertEquals("Cannot update witness once it has been set.", ex.getMessage());
        connection.lock.writeLock().unlock();
    }

    @Test
    void testSetWitnessThrowsWhenNotWriteLocked() {
        var ex = assertThrows(WriteLockException.class, () -> connection.setWitness(PeerUtils.witnessId()));
        assertEquals("setWitness", ex.operationName());
    }

    @Test
    void testSetStatus() {
        connection.lock.writeLock().lock();
        assertDoesNotThrow(() -> connection.setStatus(ConnectionImpl.Status.OPEN));
        connection.lock.writeLock().unlock();
        assertEquals(ConnectionImpl.Status.OPEN, connection.status());
    }

    @Test
    void testSetStatusThrowsWhenNotWriteLocked() {
        var ex = assertThrows(WriteLockException.class, () -> connection.setStatus(ConnectionImpl.Status.OPEN));
        assertEquals("setStatus", ex.operationName());
    }

    private static Stream<Arguments> creationPermutations() {
        return Stream.of(
                Arguments.of(
                        id, PeerUtils.senderId(), PeerUtils.receiverId(), status,
                        new TestConnection(id, PeerUtils.senderId(), Optional.empty(), PeerUtils.receiverId(), status)),
                Arguments.of(null, PeerUtils.senderId(), PeerUtils.receiverId(), status, null),
                Arguments.of(id, null, PeerUtils.receiverId(), status, null),
                Arguments.of(id, PeerUtils.senderId(), null, status, null),
                Arguments.of(id, PeerUtils.senderId(), PeerUtils.receiverId(), null, null)
        );
    }

}