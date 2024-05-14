package com.github.arobie1992.clarinet.connection;

import java.util.Objects;
import java.util.UUID;

public class ConnectionId {
    private final UUID uuid;

    ConnectionId(UUID uuid) {
        this.uuid = uuid;
    }

    public static ConnectionId random() {
        return new ConnectionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return uuid.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionId that = (ConnectionId) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}
