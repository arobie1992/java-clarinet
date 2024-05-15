package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionStatus;
import com.github.arobie1992.clarinet.connection.NoSuchConnectionException;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class InMemoryConnectionStoreTest {

    private InMemoryConnectionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryConnectionStore();
    }

    @Test
    void testCreateCollision() {
        var connectionId = ConnectionId.random();
        try(MockedStatic<ConnectionId> conn = mockStatic(ConnectionId.class)) {
            conn.when(ConnectionId::random).thenReturn(connectionId);
            assertDoesNotThrow(() -> store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN));
            assertThrows(ExistingConnectionIdException.class, () -> store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN));
        }
    }

    @Test
    void testUpdate() {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        store.read(connectionId, conn -> {
            assertEquals(connectionId, conn.connectionId());
            assertEquals(ConnectionStatus.OPEN, conn.status());
            assertEquals(PeerUtils.sender(), conn.sender());
            assertTrue(conn.witness().isEmpty());
            assertEquals(PeerUtils.receiver(), conn.receiver());
        });
        store.update(connectionId, conn -> conn.updateStatus(ConnectionStatus.CLOSED));
        store.read(connectionId, conn -> {
            assertEquals(connectionId, conn.connectionId());
            assertEquals(ConnectionStatus.CLOSED, conn.status());
            assertEquals(PeerUtils.sender(), conn.sender());
            assertTrue(conn.witness().isEmpty());
            assertEquals(PeerUtils.receiver(), conn.receiver());
        });
    }

    @Test
    void testUpdateRollback() {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        store.read(connectionId, conn -> {
            assertEquals(connectionId, conn.connectionId());
            assertEquals(ConnectionStatus.OPEN, conn.status());
            assertEquals(PeerUtils.sender(), conn.sender());
            assertTrue(conn.witness().isEmpty());
            assertEquals(PeerUtils.receiver(), conn.receiver());
        });
        assertThrows(RuntimeException.class, () -> store.update(connectionId, conn -> {
            conn.updateStatus(ConnectionStatus.CLOSED);
            throw new RuntimeException("test ex");
        }));
        store.read(connectionId, conn -> {
            assertEquals(connectionId, conn.connectionId());
            assertEquals(ConnectionStatus.OPEN, conn.status());
            assertEquals(PeerUtils.sender(), conn.sender());
            assertTrue(conn.witness().isEmpty());
            assertEquals(PeerUtils.receiver(), conn.receiver());
        });
    }

    @Test
    void testUpdateNull() {
        assertThrows(NoSuchConnectionException.class, () -> store.update(ConnectionId.random(), null));
    }

    @Test
    void testAll() {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        assertEquals(Set.of(connectionId), store.all());
    }

    @Test
    void testReadNull() {
        store.read(ConnectionId.random(), Assertions::assertNull);
    }

    @Test
    void testReadDoesNotPermitModification() {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        assertThrows(UnsupportedOperationException.class, () -> store.read(connectionId, conn -> conn.updateStatus(ConnectionStatus.CLOSED)));
    }

}