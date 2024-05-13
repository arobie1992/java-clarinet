package com.github.arobie1992.clarinet.connection;

public class WitnessSelectionException extends RuntimeException {
    private final ConnectionId connectionId;

    public WitnessSelectionException(ConnectionId connectionId) {
        super(String.format("Failed to find witness for connection %s", connectionId));
        this.connectionId = connectionId;
    }

    public ConnectionId getConnectionId() {
        return connectionId;
    }
}
