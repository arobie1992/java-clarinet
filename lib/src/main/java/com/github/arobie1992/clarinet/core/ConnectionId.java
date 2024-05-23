package com.github.arobie1992.clarinet.core;

import java.util.Objects;
import java.util.UUID;

public class ConnectionId {
    private final UUID id;

    private ConnectionId(UUID id) {
        this.id = id;
    }

    static ConnectionId random() {
        return new ConnectionId(UUID.randomUUID());
    }

    public static ConnectionId fromString(String id) {
        return new ConnectionId(UUID.fromString(id));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionId that = (ConnectionId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
