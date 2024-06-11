package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionObtainExceptionTest {

    private final ConnectionId connectionId = ConnectionId.random();

    @Test
    void testDurationConstructor() {
        var timeout = Duration.ofSeconds(10);
        var exception = new ConnectionObtainException(connectionId, timeout);
        assertEquals(connectionId, exception.connectionId());
        assertEquals("Failed to obtain connection " + connectionId + " after " + timeout, exception.getMessage());
    }

    @Test
    void testCauseConstructor() {
        var cause = new NullPointerException();
        var exception = new ConnectionObtainException(connectionId, cause);
        assertEquals(cause, exception.getCause());
        assertEquals(connectionId, exception.connectionId());
        assertEquals("Failed to obtain connection " + connectionId + " due to error " + cause.getMessage(), exception.getMessage());
    }

    @Test
    void testCauseConstructorNullCause() {
        var exception = new ConnectionObtainException(connectionId, (Throwable) null);
        assertNull(exception.getCause());
        assertEquals(connectionId, exception.connectionId());
        assertEquals("Failed to obtain connection " + connectionId + " due to error null", exception.getMessage());
    }

}