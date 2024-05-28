package com.github.arobie1992.clarinet.core;

import java.util.List;

public class ConnectionStatusException extends RuntimeException {
    private final ConnectionId connectionId;
    private final String operation;
    private final Connection.Status status;
    private final List<Connection.Status> permittedStatuses;

    public ConnectionStatusException(
            ConnectionId connectionId,
            String operation,
            Connection.Status status,
            List<Connection.Status> permittedStatuses
    ) {
        super("Connection " + connectionId + " must be in " + permittedStatuses + " to perform " + operation + " but was in " + status);
        this.connectionId = connectionId;
        this.operation = operation;
        this.status = status;
        this.permittedStatuses = List.copyOf(permittedStatuses);
    }

    public ConnectionId connectionId() {
        return connectionId;
    }

    public String operation() {
        return operation;
    }

    public Connection.Status status() {
        return status;
    }

    public List<Connection.Status> permittedStatuses() {
        return permittedStatuses;
    }
}
