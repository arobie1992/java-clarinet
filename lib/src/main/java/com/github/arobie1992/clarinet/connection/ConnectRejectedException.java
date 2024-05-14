package com.github.arobie1992.clarinet.connection;

import java.util.List;

public class ConnectRejectedException extends RuntimeException {
    private final ConnectionId connectionId;
    private final List<String> rejectReasons;

    public ConnectRejectedException(ConnectionId connectionId, List<String> rejectReasons) {
        super(String.format("Encountered errors while requesting connection %s: %s", connectionId, rejectReasons));
        this.connectionId = connectionId;
        this.rejectReasons = List.copyOf(rejectReasons);
    }

    public ConnectionId connectionId() {
        return connectionId;
    }

    public List<String> rejectReasons() {
        return rejectReasons;
    }
}
