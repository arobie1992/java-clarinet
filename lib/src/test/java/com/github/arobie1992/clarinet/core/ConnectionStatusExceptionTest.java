package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionStatusExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final String operation = "send";
    private final Connection.Status status = Connection.Status.OPEN;
    private final List<Connection.Status> permittedStatuses = List.of(Connection.Status.CLOSED);
    private final ConnectionStatusException exception = new ConnectionStatusException(connectionId, operation, status, permittedStatuses);

    @Test
    void testMessage() {
        var expected = "Connection " + connectionId + " must be in " + permittedStatuses + " to perform " + operation + " but was in " + status;
        assertEquals(expected, exception.getMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, exception.connectionId());
    }

    @Test
    void testOperation() {
        assertEquals(operation, exception.operation());
    }

    @Test
    void testStatus() {
        assertEquals(status, exception.status());
    }

    @Test
    void testPermittedStatuses() {
        assertEquals(permittedStatuses, exception.permittedStatuses());
    }

}