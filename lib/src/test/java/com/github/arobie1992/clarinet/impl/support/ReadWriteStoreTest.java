package com.github.arobie1992.clarinet.impl.support;

import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ReadWriteStoreTest {

    private ReadWriteStore<String, String> store;

    @BeforeEach
    void setUp() {
        store = new ReadWriteStore<>();
    }

    @Test
    void testSameWritesBlock() throws InterruptedException {
        var key = "1";
        var value1 = "val";
        var value2 = "val2";

        var signal = new CountDownLatch(1);
        var t1 = AsyncAssert.started(() -> store.write(key, v -> {
            signal.countDown();
            ThreadUtils.uncheckedSleep(1000);
            return value1;
        }));

        signal.await();
        var t2 = AsyncAssert.started(() -> store.write(key, v -> {
            assertEquals(value1, v);
            return value2;
        }));

        t2.join();
        t1.join();
        store.read(key, v -> assertEquals(value2, v));
    }

    @Test
    void testDifferentWritesDoNotBlock() throws InterruptedException {
        var key1 = "1";
        var value1 = "val";

        var key2 = "2";
        var value2 = "val2";

        var signal = new CountDownLatch(1);
        var t1Finished = new AtomicBoolean(false);
        var t1 = AsyncAssert.started(() -> store.write(key1, v -> {
            signal.countDown();
            ThreadUtils.uncheckedSleep(1000);
            t1Finished.set(true);
            return value1;
        }));

        signal.await();
        var t2 = AsyncAssert.started(() -> store.write(key2, v -> {
            assertFalse(t1Finished.get());
            return value2;
        }));

        t2.join();
        t1.join();
        store.read(key1, v -> assertEquals(value1, v));
        store.read(key2, v -> assertEquals(value2, v));
    }

    @Test
    void testWriteBlocksRead() throws InterruptedException {
        var key = "1";
        var value = "val";

        var signal = new CountDownLatch(1);
        var t1 = AsyncAssert.started(() -> store.write(key, v -> {
            signal.countDown();
            ThreadUtils.uncheckedSleep(1000);
            return value;
        }));

        signal.await();
        var t2 = AsyncAssert.started(() -> store.read(key, v -> assertEquals(value, v)));

        t2.join();
        t1.join();
    }

    @Test
    void testReadBlocksWrite() throws InterruptedException {
        var key = "1";
        var value = "val";

        var signal = new CountDownLatch(1);
        var t1Finished = new AtomicBoolean(false);
        var t1 = AsyncAssert.started(() -> store.read(key, v -> {
            signal.countDown();
            ThreadUtils.uncheckedSleep(1000);
            t1Finished.set(true);
        }));

        signal.await();
        var t2 = AsyncAssert.started(() -> store.write(key, v -> {
            assertTrue(t1Finished.get());
            return value;
        }));

        t2.join();
        t1.join();
        store.read(key, v -> assertEquals(value, v));
    }

    @Test
    void testReadsDoNotBlock() throws InterruptedException {
        var key = "1";

        var signal = new CountDownLatch(1);
        var t1Finished = new AtomicBoolean(false);
        var t1 = AsyncAssert.started(() -> store.read(key, v -> {
            signal.countDown();
            ThreadUtils.uncheckedSleep(1000);
            t1Finished.set(true);
        }));

        signal.await();
        var t2 = AsyncAssert.started(() -> store.read(key, v -> assertFalse(t1Finished.get())));

        t2.join();
        t1.join();
    }
}