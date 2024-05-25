package com.github.arobie1992.clarinet.core;

public class NoSuchConnectionException extends RuntimeException {
    private final ConnectionId connectionId;

    public NoSuchConnectionException(ConnectionId connectionId) {
        super("No such connection: " + connectionId);
        this.connectionId = connectionId;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }
}
