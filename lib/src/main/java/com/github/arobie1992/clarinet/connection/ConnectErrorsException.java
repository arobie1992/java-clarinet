package com.github.arobie1992.clarinet.connection;

import java.util.List;

public class ConnectErrorsException extends RuntimeException {
    private final ConnectionId connectionId;
    private final List<String> errors;

    public ConnectErrorsException(ConnectionId connectionId, List<String> errors) {
        super(String.format("Encountered errors while requesting connection %s: %s", connectionId, errors));
        this.connectionId = connectionId;
        this.errors = List.copyOf(errors);
    }

    public ConnectionId connectionId() {
        return connectionId;
    }

    public List<String> errors() {
        return errors;
    }
}
