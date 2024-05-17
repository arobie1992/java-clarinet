package com.github.arobie1992.clarinet.connection;

public class ExistingConnectionIdException extends RuntimeException {
    private final ConnectionId connectionId;

    ExistingConnectionIdException(ConnectionId connectionId) {
        super(String.format("The ConnectionID %s already exists", connectionId));
        this.connectionId = connectionId;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }
}