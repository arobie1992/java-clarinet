package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoSuchConnectionExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final NoSuchConnectionException exception = new NoSuchConnectionException(connectionId);

    @Test
    void testMessage() {
        assertEquals("No such connection: " + connectionId, exception.getMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, exception.connectionId());
    }

}