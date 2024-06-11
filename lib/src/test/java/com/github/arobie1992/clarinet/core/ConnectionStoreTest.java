package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;
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
        try(var ref = connectionStore.create(PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN)) {
            if(!(ref instanceof Writeable(ConnectionImpl conn))) {
                throw new IllegalStateException("Something went wrong when creating connection");
            }
            connectionId = conn.id();
         }
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
    void testAcceptCollision() {
        assertThrows(
                ExistingConnectionIdException.class,
                () -> connectionStore.accept(connectionId, PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN)
        );
    }

    @Test
    void testAccept() {
        var connectionId = ConnectionId.random();
        connectionStore.accept(connectionId, PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        try(var ref = connectionStore.findForRead(connectionId)) {
            if (ref instanceof Connection.Readable(Connection connection)) {
                new TestConnection(connectionId, PeerUtils.senderId(), Optional.empty(), PeerUtils.receiverId(), Connection.Status.OPEN)
                        .assertMatches(connection);
            } else {
                fail("Was expecting ConnectionStore.Readable but got " + ref.getClass());
            }
        }
    }

    @Test
    void testFindForReadAbsent() {
        try(var ref = connectionStore.findForRead(ConnectionId.random())) {
            assertEquals(Connection.Absent.class, ref.getClass());
        }
    }

    @Test
    void testFindForRead() {
        try(var ref = connectionStore.findForRead(connectionId)) {
            if (ref instanceof Connection.Readable(Connection connection)) {
                new TestConnection(connectionId, PeerUtils.senderId(), Optional.empty(), PeerUtils.receiverId(), Connection.Status.OPEN)
                        .assertMatches(connection);
            } else {
                fail("Was expecting ConnectionStore.Readable but got " + ref.getClass());
            }
        }
    }

    @Test
    void testFindForReadTimeout() throws Throwable {
        var duration = Duration.ofSeconds(1);
        runTimeoutTest(connectionId -> connectionStore.findForRead(connectionId, duration), duration);
    }

    @Test
    void testFindForReadDefaultTimeout() throws Throwable {
        runTimeoutTest(connectionStore::findForRead, Duration.ofSeconds(10));
    }

    @Test
    void testFindForWriteAbsent() {
        try(var ref = connectionStore.findForWrite(ConnectionId.random())) {
            assertEquals(Connection.Absent.class, ref.getClass());
        }
    }

    @Test
    void testFindForWriteTimeout() throws Throwable {
        var duration = Duration.ofSeconds(1);
        runTimeoutTest(connectionId -> connectionStore.findForWrite(connectionId, duration), duration);
    }

    @Test
    void testFindForWriteDefaultTimeout() throws Throwable {
        runTimeoutTest(connectionStore::findForWrite, Duration.ofSeconds(10));
    }

    private void runTimeoutTest(Function<ConnectionId, Connection.Reference> task, Duration expected) throws Throwable {
        var latch = new CountDownLatch(1);
        var t1 = AsyncAssert.started(() -> {
            // have to do write because it's the only one that's exclusive
            try(var ignored = connectionStore.findForWrite(connectionId)) {
                latch.countDown();
                Thread.sleep(expected.toMillis() + 1000);
            }
        });

        latch.await();
        var start = LocalDateTime.now();
        assertThrows(ConnectionObtainException.class, () -> task.apply(connectionId));
        var end = LocalDateTime.now();

        var actual = Duration.between(start, end);
        var low = expected.minusSeconds(1);
        var high = expected.plusSeconds(1);
        assertTrue(low.compareTo(actual) < 0);
        assertTrue(high.compareTo(actual) > 0);
        t1.join();
    }

    private void runSyncTests(
            Function<ConnectionId, Connection.Reference> task1,
            Function<ConnectionId, Connection.Reference> task2,
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