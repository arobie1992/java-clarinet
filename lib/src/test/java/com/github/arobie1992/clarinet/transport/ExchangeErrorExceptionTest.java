package com.github.arobie1992.clarinet.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExchangeErrorExceptionTest {

    private final String error = "test error";
    private final ExchangeErrorException exception = new ExchangeErrorException(error);

    @Test
    void testMessage() {
        assertEquals("Received the following error during the exchange: " + error, exception.getMessage());
    }

    @Test
    void testErrors() {
        assertEquals(error, exception.error());
    }
}