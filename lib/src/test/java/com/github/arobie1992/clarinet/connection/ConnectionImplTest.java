package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.support.TestConnection;
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
    private static final PeerId sender = new StringPeerId("sender");
    private static final PeerId witness = new StringPeerId("witness");
    private static final PeerId receiver = new StringPeerId("receiver");
    private static final ConnectionImpl.Status status = ConnectionImpl.Status.REQUESTING_RECEIVER;

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
        var connection = new ConnectionImpl(id, sender, receiver, status);
        connection.lock.writeLock().lock();
        assertDoesNotThrow(() -> connection.setWitness(witness));
        connection.lock.writeLock().unlock();
        assertEquals(Optional.of(witness), connection.witness());
    }

    @Test
    void testSetWitnessCannotUpdate() {
        var connection = new ConnectionImpl(id, sender, receiver, status);
        connection.lock.writeLock().lock();
        connection.setWitness(witness);
        var ex = assertThrows(UnsupportedOperationException.class, () -> connection.setWitness(witness));
        assertEquals("Cannot update witness once it has been set.", ex.getMessage());
        connection.lock.writeLock().unlock();
    }

    @Test
    void testSetWitnessThrowsWhenNotWriteLocked() {
        var connection = new ConnectionImpl(id, sender, receiver, status);
        var ex = assertThrows(WriteLockException.class, () -> connection.setWitness(witness));
        assertEquals("setWitness", ex.operationName());
    }

    @Test
    void testSetStatus() {
        var connection = new ConnectionImpl(id, sender, receiver, status);
        connection.lock.writeLock().lock();
        assertDoesNotThrow(() -> connection.setStatus(ConnectionImpl.Status.OPEN));
        connection.lock.writeLock().unlock();
        assertEquals(ConnectionImpl.Status.OPEN, connection.status());
    }

    @Test
    void testSetStatusThrowsWhenNotWriteLocked() {
        var connection = new ConnectionImpl(id, sender, receiver, status);
        var ex = assertThrows(WriteLockException.class, () -> connection.setStatus(ConnectionImpl.Status.OPEN));
        assertEquals("setStatus", ex.operationName());
    }

    private static Stream<Arguments> creationPermutations() {
        return Stream.of(
                Arguments.of(id, sender, receiver, status, new TestConnection(id, sender, Optional.empty(), receiver, status)),
                Arguments.of(null, sender, receiver, status, null),
                Arguments.of(id, null, receiver, status, null),
                Arguments.of(id, sender, null, status, null),
                Arguments.of(id, sender, receiver, null, null)
        );
    }

}