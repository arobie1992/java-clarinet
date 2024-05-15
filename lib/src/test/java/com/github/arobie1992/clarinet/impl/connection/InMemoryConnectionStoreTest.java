package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionStatus;
import com.github.arobie1992.clarinet.connection.NoSuchConnectionException;
import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.ThreadUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // synchronization tests
    @Test
    void testUpdatesBlock() throws InterruptedException {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        var t1 = AsyncAssert.started(() -> store.update(connectionId, conn -> {
            assertEquals(ConnectionStatus.OPEN, conn.status());
            ThreadUtils.uncheckedSleep(1000);
            return conn.updateStatus(ConnectionStatus.CLOSING);
        }));
        var t2 = AsyncAssert.started(() -> store.update(connectionId, conn -> {
            assertEquals(ConnectionStatus.CLOSING, conn.status());
            return conn.updateStatus(ConnectionStatus.CLOSED);
        }));
        t2.join();
        t1.join();
        store.read(connectionId, conn -> assertEquals(ConnectionStatus.CLOSED, conn.status()));
    }

    @Test
    void testUpdateBlocksRead() throws InterruptedException {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        var t1 = AsyncAssert.started(() -> store.update(connectionId, conn -> {
            assertEquals(ConnectionStatus.OPEN, conn.status());
            ThreadUtils.uncheckedSleep(1000);
            return conn.updateStatus(ConnectionStatus.CLOSING);
        }));
        var t2 = AsyncAssert.started(() -> store.read(connectionId, conn -> assertEquals(ConnectionStatus.CLOSING, conn.status())));
        t2.join();
        t1.join();
    }

    @Test
    void testReadBlocksUpdate() throws InterruptedException {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        var readFinished = new AtomicBoolean(false);
        var t1 = AsyncAssert.started(() -> store.read(connectionId, conn -> {
            ThreadUtils.uncheckedSleep(1000);
            assertEquals(ConnectionStatus.OPEN, conn.status());
            readFinished.set(true);
        }));
        var t2 = AsyncAssert.started(() -> store.update(connectionId, conn -> {
            assertTrue(readFinished.get());
            return conn.updateStatus(ConnectionStatus.CLOSING);
        }));
        t2.join();
        t1.join();
    }

    @Test
    void testReadsDoNotBlock() throws InterruptedException {
        var connectionId = store.create(PeerUtils.sender(), PeerUtils.receiver(), ConnectionStatus.OPEN);
        var firstReadFinished = new AtomicBoolean(false);
        var t1 = AsyncAssert.started(() -> store.read(connectionId, conn -> {
            ThreadUtils.uncheckedSleep(1000);
            assertEquals(ConnectionStatus.OPEN, conn.status());
            firstReadFinished.set(true);
        }));
        var t2 = AsyncAssert.started(() -> store.read(connectionId, conn -> {
            assertFalse(firstReadFinished.get());
        }));
        t2.join();
        t1.join();
        assertTrue(firstReadFinished.get());
    }

}