package com.github.arobie1992.clarinet.impl.support;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReadWriteStore<K, V> {

    private final Map<K, V> backingStore = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Collection<K> keySet() {
        return backingStore.keySet();
    }

    public void read(K key, Consumer<V> readFunction) {
        try {
            lock.readLock().lock();
            V value = backingStore.get(key);
            readFunction.accept(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void write(K key, Function<V, V> writeFunction) {
        try {
            lock.writeLock().lock();
            backingStore.compute(key, (k,v) -> writeFunction.apply(v));
        } finally {
            lock.writeLock().unlock();
        }
    }
}
