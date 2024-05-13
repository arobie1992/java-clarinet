package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;

public class ExistingConnectionIDException extends RuntimeException {
    private final ConnectionId connectionID;

    public ExistingConnectionIDException(ConnectionId connectionID) {
        super(String.format("The ConnectionID %s already exists", connectionID));
        this.connectionID = connectionID;
    }

    public ConnectionId getConnectionID() {
        return connectionID;
    }
}
