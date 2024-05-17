package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WriteLockExceptionTest {

    private final String operation = "test operation";
    private final WriteLockException exception = new WriteLockException(operation);

    @Test
    void testMessage() {
        assertEquals("Write lock must be held to perform operation " + operation, exception.getMessage());
    }

    @Test
    void testOperation() {
        assertEquals(operation, exception.operationName());
    }
}