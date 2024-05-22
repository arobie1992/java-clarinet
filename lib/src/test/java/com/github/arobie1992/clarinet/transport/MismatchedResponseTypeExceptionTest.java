package com.github.arobie1992.clarinet.transport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MismatchedResponseTypeExceptionTest {

    private final String response = "This is a test response";
    @SuppressWarnings("rawtypes")
    private final Class<List> responseType = List.class;
    private final MismatchedResponseTypeException exception = new MismatchedResponseTypeException(response, responseType);

    @Test
    void testMessage() {
        assertEquals("Failed to parse response as type interface java.util.List", exception.getMessage());
    }

    @Test
    void testResponse() {
        assertEquals(response, exception.response());
    }

    @Test
    void testResponseType() {
        assertEquals(responseType, exception.responseType());
    }
}