package com.github.arobie1992.clarinet.core;

record Writeable(ConnectionImpl connection) implements WriteableReference {
    @Override
    public void close() {
        connection.lock.writeLock().unlock();
    }
}
