package com.github.arobie1992.clarinet.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class QueryResponseTest {
    @Test
    void testNullArgs() {
        var queryResponse = new QueryResponse(null, null, null);
        assertNull(queryResponse.hash());
        assertNull(queryResponse.signature());
        assertNull(queryResponse.hashAlgorithm());
    }
}