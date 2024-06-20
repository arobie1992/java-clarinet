package com.github.arobie1992.clarinet.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageSummaryTest {
    @Test
    void testNullMessageId() {
        assertThrows(NullPointerException.class, () -> new MessageSummary(null, null, null, null));
    }
}