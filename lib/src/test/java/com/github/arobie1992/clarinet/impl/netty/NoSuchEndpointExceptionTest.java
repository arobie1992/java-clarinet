package com.github.arobie1992.clarinet.impl.netty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoSuchEndpointExceptionTest {

    private final String endpoint = "myTestEndpoint";
    private final NoSuchEndpointException noSuchEndpointException = new NoSuchEndpointException(endpoint);

    @Test
    void testMessage() {
        assertEquals("No such endpoint: myTestEndpoint", noSuchEndpointException.getMessage());
    }

    @Test
    void testEndpoint() {
        assertEquals(endpoint, noSuchEndpointException.endpoint());
    }

}