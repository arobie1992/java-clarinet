package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExistingConnectionIdExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final ExistingConnectionIdException exception = new ExistingConnectionIdException(connectionId);

    @Test
    void testMessage() {
        assertEquals(String.format("The ConnectionID %s already exists", connectionId), exception.getMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, exception.connectionId());
    }

}