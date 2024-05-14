package com.github.arobie1992.clarinet.connection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConnectErrorsExceptionTests {

    private final ConnectionId connId = ConnectionId.random();
    private final List<String> errs = List.of("Test error");
    private final ConnectErrorsException ex = new ConnectErrorsException(connId, errs);

    @Test
    void testMessage() {
        assertEquals(String.format("Encountered errors while requesting connection %s: %s", connId, errs), ex.getLocalizedMessage());
    }

    @Test
    void testConnectionId() {
        assertEquals(connId, ex.connectionId());
    }

    @Test
    void testErrors() {
        var exErrs = ex.errors();
        assertEquals(errs, exErrs);
        assertThrows(UnsupportedOperationException.class, () -> ex.errors().add("Test error2"));
    }

}
