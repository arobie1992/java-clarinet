package com.github.arobie1992.clarinet.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class QueryResponseTest {

    private final byte[] expectedHash = {1};
    private final byte[] expectedSig = {2};

    private byte[] hash;
    private byte[] sig;
    private QueryResponse queryResponse;

    @BeforeEach
    void setUp() {
        hash = Arrays.copyOf(expectedHash, expectedHash.length);
        sig = Arrays.copyOf(expectedSig, expectedSig.length);
        queryResponse = new QueryResponse(hash, sig, "alg");
        assertArrayEquals(expectedHash, hash);
        assertArrayEquals(expectedSig, sig);
    }

    @Test
    void testCopyIn() {
        assertArrayEquals(hash, queryResponse.hash());
        hash[0] = 55;
        assertFalse(Arrays.equals(expectedHash, hash));
        assertArrayEquals(expectedHash, queryResponse.hash());

        assertArrayEquals(expectedSig, queryResponse.signature());
        sig[0] = 21;
        assertFalse(Arrays.equals(expectedSig, sig));
        assertArrayEquals(expectedSig, queryResponse.signature());
    }

    @Test
    void testCopyOut() {
        // intellij was complaining about hash and sig potentially being null and this is easy enough to get it to be quiet.
        var hash = Objects.requireNonNull(queryResponse.hash());
        assertArrayEquals(expectedHash, hash);
        hash[0] = 43;
        assertFalse(Arrays.equals(expectedHash, hash));

        var sig = Objects.requireNonNull(queryResponse.signature());
        assertArrayEquals(expectedSig, sig);
        sig[0] = 99;
        assertFalse(Arrays.equals(expectedSig, sig));
        assertArrayEquals(expectedSig, queryResponse.signature());
    }

    @Test
    void testNullArgs() {
        queryResponse = new QueryResponse(null, null, null);
        assertNull(queryResponse.hash());
        assertNull(queryResponse.signature());
        assertNull(queryResponse.hashAlgorithm());
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    void testEqualsAndHashCodeReflexive() {
        assertEquals(queryResponse, queryResponse);
        assertEquals(queryResponse.hashCode(), queryResponse.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSymmetric() {
        var matching = new QueryResponse(hash, sig, "alg");
        assertNotSame(queryResponse, matching);
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());
        assertEquals(matching, queryResponse);
        assertEquals(matching.hashCode(), queryResponse.hashCode());
    }

    @Test
    void testEqualsAndHashCodeTransitive() {
        var matching = new QueryResponse(hash, sig, "alg");
        var matching2 = new QueryResponse(hash, sig, "alg");

        assertNotSame(queryResponse, matching);
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());

        assertNotSame(matching, matching2);
        assertEquals(matching, matching2);
        assertEquals(matching.hashCode(), matching2.hashCode());

        assertNotSame(queryResponse, matching2);
        assertEquals(queryResponse, matching2);
        assertEquals(queryResponse.hashCode(), matching2.hashCode());
    }

    @Test
    void testEqualsAndHashCodeConsistent() {
        var matching = new QueryResponse(hash, sig, "alg");
        assertNotSame(queryResponse, matching);
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());
    }

    @Test
    void testEqualsNull() {
        //noinspection ConstantValue, SimplifiableAssertion
        assertFalse(queryResponse.equals(null));
    }

    @Test
    void testEqualsAndHashCodeHashMatching() {
        var matching = new QueryResponse(Arrays.copyOf(hash, hash.length), sig, "alg");
        assertNotSame(queryResponse, matching);
        assertNotSame(queryResponse.hash(), matching.hash());
        assertNotEquals(queryResponse.hash(), matching.hash());
        assertArrayEquals(queryResponse.hash(), matching.hash());
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());
    }

    @Test
    void testEqualsAndHashCodeHashNotMatching() {
        var notMatching = new QueryResponse(new byte[]{97}, sig, "alg");
        assertNotSame(queryResponse, notMatching);
        assertNotSame(queryResponse.hash(), notMatching.hash());
        assertNotEquals(queryResponse.hash(), notMatching.hash());
        assertFalse(Arrays.equals(queryResponse.hash(), notMatching.hash()));
        assertNotEquals(queryResponse, notMatching);
        assertNotEquals(queryResponse.hashCode(), notMatching.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSigMatching() {
        var matching = new QueryResponse(hash, Arrays.copyOf(sig, sig.length), "alg");
        assertNotSame(queryResponse, matching);
        assertNotSame(queryResponse.signature(), matching.signature());
        assertNotEquals(queryResponse.signature(), matching.signature());
        assertArrayEquals(queryResponse.signature(), matching.signature());
        assertEquals(queryResponse, matching);
        assertEquals(queryResponse.hashCode(), matching.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSigNotMatching() {
        var notMatching = new QueryResponse(hash, new byte[]{79}, "alg");
        assertNotSame(queryResponse, notMatching);
        assertNotSame(queryResponse.signature(), notMatching.signature());
        assertNotEquals(queryResponse.signature(), notMatching.signature());
        assertFalse(Arrays.equals(queryResponse.signature(), notMatching.signature()));
        assertNotEquals(queryResponse, notMatching);
        assertNotEquals(queryResponse.hashCode(), notMatching.hashCode());
    }

    @Test
    void testEqualsAndHashCodeAlgNotMatching() {
        var notMatching = new QueryResponse(hash, sig, "difAlg");
        assertNotSame(queryResponse, notMatching);
        assertNotEquals(queryResponse.hashAlgorithm(), notMatching.hashAlgorithm());
        assertNotEquals(queryResponse, notMatching);
        assertNotEquals(queryResponse.hashCode(), notMatching.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDiffClass() {
        var notMatching = new Object();
        assertNotEquals(queryResponse, notMatching);
        assertNotEquals(queryResponse.hashCode(), notMatching.hashCode());
    }
}