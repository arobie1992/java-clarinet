package com.github.arobie1992.clarinet.connection;

public class NoSuchConnectionException extends RuntimeException {
    private final ConnectionId connectionId;

    public NoSuchConnectionException(ConnectionId connectionId) {
        super(String.format("No connection with ID %s found.", connectionId));
        this.connectionId = connectionId;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }
}
