package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryForwardTest {
    @Test
    void testNullQueryResponse() {
        assertThrows(NullPointerException.class, () -> new QueryForward(null, Bytes.of(new byte[]{3,4,2,1})));
    }
    @Test
    void testNullSignature() {
        assertThrows(NullPointerException.class, () -> new QueryForward(
                new QueryResponse(Bytes.of(new byte[]{1}), Bytes.of(new byte[]{2}), "SHA-256"),
                null
        ));
    }
}