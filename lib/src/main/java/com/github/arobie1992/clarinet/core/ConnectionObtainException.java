package com.github.arobie1992.clarinet.core;

import java.time.Duration;

public class ConnectionObtainException extends RuntimeException {
    private final ConnectionId connectionId;

    public ConnectionObtainException(ConnectionId connectionId, Throwable cause) {
        super("Failed to obtain connection " + connectionId + " due to error " + (cause == null ? null : cause.getMessage()), cause);
        this.connectionId = connectionId;
    }

    public ConnectionObtainException(ConnectionId connectionId, Duration timeout) {
        super("Failed to obtain connection " + connectionId + " after " + timeout);
        this.connectionId = connectionId;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }
}
