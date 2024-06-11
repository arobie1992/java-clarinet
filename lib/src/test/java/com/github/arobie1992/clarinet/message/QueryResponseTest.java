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

}