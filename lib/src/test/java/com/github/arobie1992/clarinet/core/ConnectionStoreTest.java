package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.query.QueryTerms;
import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.testutils.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    private ConnectionId create(Connection.Status status) {
        try(var ref = connectionStore.create(PeerUtils.senderId(), PeerUtils.receiverId(), status)) {
            if(!(ref instanceof Writeable(ConnectionImpl conn))) {
                throw new IllegalStateException("Something went wrong when creating connection");
            }
            return conn.id();
        }
    }

    @Test
    void testQuery() throws Throwable {
        ConnectionId id1 = create(Connection.Status.REQUESTING_WITNESS);
        // used closed so it excludes connectionId
        ConnectionId id2 = create(Connection.Status.CLOSED);
        ConnectionId id3 = create(Connection.Status.CLOSED);
        var expected = List.of(id2, id3);

        Comparator<Connection> cmp = (o1, o2) -> {
            var i1 = expected.indexOf(o1.id());
            var i2 = expected.indexOf(o2.id());
            return Integer.compare(i1, i2);
        };

        var found = new ArrayList<ConnectionId>();
        var sleepStart = new AtomicReference<LocalDateTime>();
        var sleepEnd = new AtomicReference<LocalDateTime>();
        var latch = new CountDownLatch(1);
        var t1 = AsyncAssert.started(() -> connectionStore.query(new QueryTerms<>(c -> c.status().equals(Connection.Status.CLOSED), cmp))
                .forEach(ref -> {
                    try(ref) {
                        found.add(ref.connection().id());
                        if(ref.connection().id().equals(id2)) {
                            sleepStart.set(LocalDateTime.now());
                            latch.countDown();
                            ThreadUtils.sleepUnchecked(2000);
                            sleepEnd.set(LocalDateTime.now());
                        } else if(ref.connection().id().equals(id1)) {
                            fail("Query should not have found id1");
                        } else if(ref.connection().id().equals(connectionId)) {
                            fail("Query should not have found connectionId");
                        }
                    }
                }));

        var id2Read = new AtomicReference<LocalDateTime>();
        var t2 = AsyncAssert.started(() -> {
            latch.await();
            try(var ignored = connectionStore.findForRead(id2)) {
                id2Read.set(LocalDateTime.now());
            }
        });
        var id2Write = new AtomicReference<LocalDateTime>();
        var t3 = AsyncAssert.started(() -> {
            latch.await();
            try(var ignored = connectionStore.findForWrite(id2)) {
                id2Write.set(LocalDateTime.now());
            }
        });
        var id3Write = new AtomicReference<LocalDateTime>();
        var t4 = AsyncAssert.started(() -> {
            latch.await();
            try(var ignored = connectionStore.findForWrite(id3)) {
                id3Write.set(LocalDateTime.now());
            }
        });
        var id1Write = new AtomicReference<LocalDateTime>();
        var t5 = AsyncAssert.started(() -> {
            latch.await();
            try(var ignored = connectionStore.findForWrite(id1)) {
                id1Write.set(LocalDateTime.now());
            }
        });
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();

        assertEquals(List.of(id2, id3), found);
        // id2 can still be read while stream is processing
        assertBetween(id2Read.get(), sleepStart.get(), sleepEnd.get());
        // id2 write must wait for processing to complete
        assertTrue(id2Write.get().isAfter(sleepEnd.get()));
        // id3 can still be written while 2 is being processed
        assertBetween(id3Write.get(), sleepStart.get(), sleepEnd.get());
        // id1 should be totally unaffected
        assertBetween(id1Write.get(), sleepStart.get(), sleepEnd.get());

        for(var id : List.of(id1, id2, id3, connectionId)) {
            //noinspection EmptyTryBlock
            try(var ignored = connectionStore.findForWrite(id, Duration.ofSeconds(1))) {}
            catch(ConnectionObtainException e) {
                fail("Connection " + id + " was not correctly unlocked");
            }
        }
    }

    private void assertBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        assertTrue(dateTime.isAfter(start), "dateTime: " + dateTime + " not after start: " + start);
        assertTrue(dateTime.isBefore(end), "dateTime: " + dateTime + " not before end: " + end);
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