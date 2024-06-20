package com.github.arobie1992.clarinet.message;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class QueryForwardTest {

    private final QueryResponse queryResponse = new QueryResponse(new byte[]{3,5,2}, new byte[]{46}, "SHA-256");
    private final QueryForward queryForward = new QueryForward(queryResponse, new byte[]{9,52});

    @Test
    void testEqualsAndHashCodeReflexive() {
        //noinspection EqualsWithItself
        assertEquals(queryForward, queryForward);
        assertEquals(queryForward.hashCode(), queryForward.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSymmetric() {
        var b = new QueryForward(queryResponse, queryForward.signature());
        assertNotSame(queryForward, b);
        assertEquals(queryForward, b);
        assertEquals(queryForward.hashCode(), b.hashCode());
        assertEquals(b, queryForward);
        assertEquals(b.hashCode(), queryForward.hashCode());
    }

    @Test
    void testEqualsTransitive() {
        var b = new QueryForward(queryResponse, queryForward.signature());
        var c = new QueryForward(queryResponse, queryForward.signature());
        assertNotSame(queryForward, b);
        assertEquals(queryForward, b);
        assertEquals(queryForward.hashCode(), b.hashCode());

        assertNotSame(b, c);
        assertEquals(b, c);
        assertEquals(b.hashCode(), c.hashCode());

        assertNotSame(queryForward, c);
        assertEquals(queryForward, c);
        assertEquals(queryForward.hashCode(), c.hashCode());
    }

    @Test
    void testEqualsConsistent() {
        var b = new QueryForward(queryResponse, queryForward.signature());
        assertNotSame(queryForward, b);
        assertEquals(queryForward, b);
        assertEquals(queryForward.hashCode(), b.hashCode());
        assertEquals(queryForward, b);
        assertEquals(queryForward.hashCode(), b.hashCode());
        assertEquals(queryForward, b);
        assertEquals(queryForward.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsNull() {
        // noinspection SimplifiableAssertion, ConstantValue
        assertFalse(queryForward.equals(null));
    }

    @Test
    void testEqualsQueryResponseNotMatching() {
        var queryResponse = new QueryResponse(new byte[]{95}, new byte[]{99,0,70}, "SHA-256");
        assertNotEquals(queryForward.queryResponse(), queryResponse);
        var notEqual = new QueryForward(queryResponse, queryForward.signature());
        assertNotEquals(queryForward, notEqual);
        assertNotEquals(queryForward.hashCode(), notEqual.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSignatureMatching() {
        var b = new QueryForward(queryResponse, Arrays.copyOf(queryForward.signature(), queryForward.signature().length));
        assertNotSame(queryForward.signature(), b.signature());
        assertNotEquals(queryForward.signature(), b.signature());
        assertArrayEquals(queryForward.signature(), b.signature());
        assertEquals(queryForward, b);
        assertEquals(queryForward.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDataNotMatching() {
        var sig = new byte[]{77};
        assertFalse(Arrays.equals(queryForward.signature(), sig));
        var notEqual = new QueryForward(queryResponse, sig);
        assertNotEquals(queryForward, notEqual);
        assertNotEquals(queryForward.hashCode(), notEqual.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDifferentClass() {
        var b = new Object();
        assertNotEquals(queryForward, b);
        assertNotEquals(queryForward.hashCode(), b.hashCode());
    }

}