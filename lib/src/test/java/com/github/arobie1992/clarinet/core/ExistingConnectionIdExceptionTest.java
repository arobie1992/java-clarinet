package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExistingConnectionIdExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final ExistingConnectionIdException ex = new ExistingConnectionIdException(connectionId);

    @Test
    void testMessage() {
        assertEquals(String.format("The ConnectionId %s already exists", connectionId), ex.getMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, ex.connectionId());
    }

}