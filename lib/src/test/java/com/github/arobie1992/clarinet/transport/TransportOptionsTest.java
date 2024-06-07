package com.github.arobie1992.clarinet.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransportOptionsTest {

    @Test
    void testNoArgsConstructor() {
        TransportOptions options = new TransportOptions();
        assertTrue(options.receiveTimeout().isEmpty());
        assertTrue(options.sendTimeout().isEmpty());
    }

}