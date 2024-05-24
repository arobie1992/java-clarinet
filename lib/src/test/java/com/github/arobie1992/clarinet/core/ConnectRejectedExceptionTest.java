package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectRejectedExceptionTest {

    private final String reason = "Test; reason";
    private final ConnectRejectedException connectRejectedException = new ConnectRejectedException(reason);

    @Test
    void testMessage() {
        assertEquals("Connect request rejected with given reason: " + reason, connectRejectedException.getMessage());
    }

    @Test
    void testReason() {
        assertEquals(reason, connectRejectedException.reason());
    }
}