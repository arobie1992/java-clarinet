package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionStoreTest {

    private ConnectionId connectionId = ConnectionId.random();
    private ConnectionStore connectionStore;

    @BeforeEach
    void setUp() {
        connectionStore = new ConnectionStore();
        connectionId = connectionStore.create(PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
    }

    @Test
    void testCreateCollision() {
        try(var mock = Mockito.mockStatic(ConnectionId.class)) {
            mock.when(ConnectionId::random).thenReturn(connectionId);
            assertThrows(
                    ExistingConnectionIdException.class,
                    () -> connectionStore.create(PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN)
            );
        }
    }

    @Test
    void testFindForReadAbsent() throws Exception {
        try(var ref = connectionStore.findForRead(ConnectionId.random())) {
            assertEquals(ConnectionStore.Absent.class, ref.getClass());
        }
    }

    @Test
    void testCreate() throws Exception {
        try(var ref = connectionStore.findForRead(connectionId)) {
            if (ref instanceof ConnectionStore.Readable(Connection connection)) {
                new TestConnection(connectionId, PeerUtils.senderId(), Optional.empty(), PeerUtils.receiverId(), Connection.Status.OPEN)
                        .assertMatches(connection);
            } else {
                fail("Was expecting ConnectionStore.Readable but got " + ref.getClass());
            }
        }
    }

    @Test
    void testFindForWriteAbsent() throws Exception {
        try(var ref = connectionStore.findForWrite(ConnectionId.random())) {
            assertEquals(ConnectionStore.Absent.class, ref.getClass());
        }
    }

    private void runSyncTests(
            Function<ConnectionId, ConnectionStore.ConnectionReference> task1,
            Function<ConnectionId, ConnectionStore.ConnectionReference> task2,
            boolean expectFinished
    ) throws Throwable {
        var barrier = new CountDownLatch(1);

        var firstTaskFinished = new AtomicBoolean(false);
        var t1 = AsyncAssert.started(() -> {
            try(var ignored = task1.apply(connectionId)) {
                barrier.countDown();
                Thread.sleep(1000);
                firstTaskFinished.set(true);
            }
        });

        barrier.await();
        var t2 = AsyncAssert.started(() -> {
            try(var ignored = task2.apply(connectionId)) {
                assertEquals(expectFinished, firstTaskFinished.get());
            }
        });

        t1.join();
        t2.join();
    }

    @Test
    void testWritesBlock() throws Throwable {
        runSyncTests(connectionStore::findForWrite, connectionStore::findForWrite, true);
    }

    @Test
    void testWriteBlocksRead() throws Throwable {
        runSyncTests(connectionStore::findForWrite, connectionStore::findForRead, true);
    }

    @Test
    void testReadBlocksWrite() throws Throwable {
        runSyncTests(connectionStore::findForRead, connectionStore::findForWrite, true);
    }

    @Test
    void testMultipleReadsAllowed() throws Throwable {
        runSyncTests(connectionStore::findForRead, connectionStore::findForRead, false);
    }
}