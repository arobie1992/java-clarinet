package com.github.arobie1992.clarinet.connection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class UnexpectedConnectionExceptionTest {
    private final ConnectionId expected = ConnectionId.random();
    private final ConnectionId actual = ConnectionId.random();
    private final UnexpectedConnectionException exception = new UnexpectedConnectionException(expected, actual);

    @Test
    void testMessage() {
        assertEquals(String.format("Unexpected connection: expected: %s, actual: %s", expected, actual), exception.getMessage());
    }

    @Test
    void testExpected() {
        assertEquals(expected, exception.expected());
    }

    @Test
    void testActual() {
        assertEquals(actual, exception.actual());
    }
}
