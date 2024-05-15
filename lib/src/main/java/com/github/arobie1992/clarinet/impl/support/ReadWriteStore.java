package com.github.arobie1992.clarinet.impl.support;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReadWriteStore<K, V> {

    private static class ValueStore<V> {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        volatile V value;
    }

    private final Map<K, ValueStore<V>> backingStore = new ConcurrentHashMap<>();

    public Collection<K> keySet() {
        return backingStore.keySet();
    }

    public void read(K key, Consumer<V> readFunction) {
        backingStore.computeIfAbsent(key, k -> new ValueStore<>());
        ValueStore<V> valueStore = backingStore.get(key);
        try {
            valueStore.lock.readLock().lock();
            readFunction.accept(valueStore.value);
        } finally {
            valueStore.lock.readLock().unlock();
        }
    }

    public void write(K key, Function<V, V> writeFunction) {
        backingStore.compute(key, (k, v) -> {
            if(v == null) {
                v = new ValueStore<>();
            }
            try {
                v.lock.writeLock().lock();
                v.value = writeFunction.apply(v.value);
                return v;
            } finally {
                v.lock.writeLock().unlock();
            }
        });
    }
}
