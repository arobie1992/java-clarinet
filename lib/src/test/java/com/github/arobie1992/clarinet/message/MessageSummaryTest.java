package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MessageSummaryTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final MessageId messageId = new MessageId(connectionId, 0);
    private final byte[] hash = new byte[]{99,2,70};
    private final String hashAlg = "SHA-256";
    private final byte[] sig = new byte[]{54};
    private final MessageSummary summary = new MessageSummary(messageId, hash, hashAlg, sig);

    @Test
    void testEqualsAndHashCodeReflexive() {
        //noinspection EqualsWithItself
        assertEquals(summary, summary);
        assertEquals(summary.hashCode(), summary.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSymmetric() {
        var b = new MessageSummary(messageId, summary.hash(), summary.hashAlgorithm(), summary.witnessSignature());
        assertNotSame(summary, b);
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());
        assertEquals(b, summary);
        assertEquals(b.hashCode(), summary.hashCode());
    }

    @Test
    void testEqualsTransitive() {
        var b = new MessageSummary(messageId, summary.hash(), summary.hashAlgorithm(), summary.witnessSignature());
        var c = new MessageSummary(messageId, summary.hash(), summary.hashAlgorithm(), summary.witnessSignature());
        assertNotSame(summary, b);
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());

        assertNotSame(b, c);
        assertEquals(b, c);
        assertEquals(b.hashCode(), c.hashCode());

        assertNotSame(summary, c);
        assertEquals(summary, c);
        assertEquals(summary.hashCode(), c.hashCode());
    }

    @Test
    void testEqualsConsistent() {
        var b = new MessageSummary(messageId, summary.hash(), summary.hashAlgorithm(), summary.witnessSignature());
        assertNotSame(summary, b);
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsNull() {
        // noinspection SimplifiableAssertion, ConstantValue
        assertFalse(summary.equals(null));
    }

    @Test
    void testEqualsMessageIdNotMatching() {
        var messageId = new MessageId(connectionId, 2);
        assertNotEquals(summary.messageId(), messageId);
        var notEqual = new MessageSummary(messageId, summary.hash(), summary.hashAlgorithm(), summary.witnessSignature());
        assertNotEquals(summary, notEqual);
        assertNotEquals(summary.hashCode(), notEqual.hashCode());
    }

    @Test
    void testEqualsAndHashCodeHashMatching() {
        var b = new MessageSummary(messageId, Arrays.copyOf(hash, hash.length), hashAlg, sig);
        assertNotSame(summary.hash(), b.hash());
        assertNotEquals(summary.hash(), b.hash());
        assertArrayEquals(summary.hash(), b.hash());
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeHashNotMatching() {
        var hash = new byte[]{28};
        assertFalse(Arrays.equals(hash, summary.hash()));
        var b = new MessageSummary(messageId, hash, hashAlg, sig);
        assertNotEquals(summary, b);
        assertNotEquals(summary.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeHashAlgNotMatching() {
        var hashAlg = "Hi";
        assertNotEquals(summary.hashAlgorithm(), hashAlg);
        var notEqual = new MessageSummary(messageId, Arrays.copyOf(hash, hash.length), hashAlg, sig);
        assertNotEquals(summary, notEqual);
        assertNotEquals(summary.hashCode(), notEqual.hashCode());
    }

    @Test
    void testEqualsAndHashCodeWitnessSigMatching() {
        var b = new MessageSummary(messageId, hash, hashAlg, Arrays.copyOf(sig, sig.length));
        assertNotSame(summary.witnessSignature(), b.witnessSignature());
        assertNotEquals(summary.witnessSignature(), b.witnessSignature());
        assertArrayEquals(summary.witnessSignature(), b.witnessSignature());
        assertEquals(summary, b);
        assertEquals(summary.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeWitnessSigNotMatching() {
        var sig = new byte[]{28};
        assertFalse(Arrays.equals(sig, summary.witnessSignature()));
        var b = new MessageSummary(messageId, hash, hashAlg, sig);
        assertNotEquals(summary, b);
        assertNotEquals(summary.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDifferentClass() {
        var b = new Object();
        assertNotEquals(summary, b);
        assertNotEquals(summary.hashCode(), b.hashCode());
    }

}