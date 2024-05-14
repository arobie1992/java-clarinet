package com.github.arobie1992.clarinet.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WitnessSelectionExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final WitnessSelectionException witnessSelectionException = new WitnessSelectionException(connectionId);

    @Test
    void testMessage() {
        assertEquals(String.format("Failed to find witness for connection %s", connectionId), witnessSelectionException.getMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, witnessSelectionException.getConnectionId());
    }

}