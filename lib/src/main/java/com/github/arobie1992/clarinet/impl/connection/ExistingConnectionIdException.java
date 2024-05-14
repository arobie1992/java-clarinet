package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;

public class ExistingConnectionIdException extends RuntimeException {
    private final ConnectionId connectionId;

    public ExistingConnectionIdException(ConnectionId connectionId) {
        super(String.format("The ConnectionID %s already exists", connectionId));
        this.connectionId = connectionId;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }
}
