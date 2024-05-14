package com.github.arobie1992.clarinet.connection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConnectRejectedExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final List<String> reasons = List.of("test reason");
    private final ConnectRejectedException exception = new ConnectRejectedException(connectionId, reasons);

    @Test
    void testMessage() {
        assertEquals(
                String.format("Encountered errors while requesting connection %s: %s", connectionId, reasons),
                exception.getMessage()
        );
    }

    @Test
    void testConnectionId() {
        assertEquals(connectionId, exception.connectionId());
    }

    @Test
    void testRejectReasons() {
        var exReasons = exception.rejectReasons();
        assertEquals(reasons, exReasons);
        assertThrows(UnsupportedOperationException.class, () -> exReasons.add("test 2"));
    }
}
