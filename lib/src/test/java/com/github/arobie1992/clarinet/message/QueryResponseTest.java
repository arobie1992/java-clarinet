package com.github.arobie1992.clarinet.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryResponseTest {
    @Test
    void testNullMessageDetails() {
        assertThrows(NullPointerException.class, () -> new QueryResponse(null, null, null));
    }
}