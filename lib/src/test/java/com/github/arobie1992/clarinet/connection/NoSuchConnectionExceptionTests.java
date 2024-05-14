package com.github.arobie1992.clarinet.connection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class NoSuchConnectionExceptionTests {

    private final ConnectionId connectionId = ConnectionId.random();
    private final NoSuchConnectionException ex = new NoSuchConnectionException(connectionId);

    @Test
    void testMessage() {
        assertEquals(String.format("No connection with ID %s found.", connectionId), ex.getMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, ex.connectionId());
    }

}
