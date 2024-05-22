package com.github.arobie1992.clarinet.transport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeErrorsExceptionTest {

    private final List<String> errors = List.of("test error");
    private final ExchangeErrorsException exception = new ExchangeErrorsException(errors);

    @Test
    void testMessage() {
        assertEquals("Received the following errors during the exchange: " + errors, exception.getMessage());
    }

    @Test
    void testErrors() {
        assertEquals(errors, exception.errors());
    }
}