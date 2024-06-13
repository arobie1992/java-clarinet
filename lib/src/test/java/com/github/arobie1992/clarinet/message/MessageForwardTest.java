package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MessageForwardTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final MessageId messageId = new MessageId(connectionId, 0);
    private final MessageSummary summary = new MessageSummary(messageId, new byte[]{99,2,70}, "SHA-256", new byte[]{54});
    private final MessageForward messageForward = new MessageForward(summary, new byte[]{9,72,10});

    @Test
    void testEqualsAndHashCodeReflexive() {
        //noinspection EqualsWithItself
        assertEquals(messageForward, messageForward);
        assertEquals(messageForward.hashCode(), messageForward.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSymmetric() {
        var b = new MessageForward(summary, messageForward.signature());
        assertNotSame(messageForward, b);
        assertEquals(messageForward, b);
        assertEquals(messageForward.hashCode(), b.hashCode());
        assertEquals(b, messageForward);
        assertEquals(b.hashCode(), messageForward.hashCode());
    }

    @Test
    void testEqualsTransitive() {
        var b = new MessageForward(summary, messageForward.signature());
        var c = new MessageForward(summary, messageForward.signature());
        assertNotSame(messageForward, b);
        assertEquals(messageForward, b);
        assertEquals(messageForward.hashCode(), b.hashCode());

        assertNotSame(b, c);
        assertEquals(b, c);
        assertEquals(b.hashCode(), c.hashCode());

        assertNotSame(messageForward, c);
        assertEquals(messageForward, c);
        assertEquals(messageForward.hashCode(), c.hashCode());
    }

    @Test
    void testEqualsConsistent() {
        var b = new MessageForward(summary, messageForward.signature());
        assertNotSame(messageForward, b);
        assertEquals(messageForward, b);
        assertEquals(messageForward.hashCode(), b.hashCode());
        assertEquals(messageForward, b);
        assertEquals(messageForward.hashCode(), b.hashCode());
        assertEquals(messageForward, b);
        assertEquals(messageForward.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsNull() {
        // noinspection SimplifiableAssertion, ConstantValue
        assertFalse(messageForward.equals(null));
    }

    @Test
    void testEqualsSummaryNotMatching() {
        var summary = new MessageSummary(messageId, new byte[]{99,0,70}, "SHA-256", new byte[]{54});
        assertNotEquals(messageForward.summary(), summary);
        var notEqual = new MessageForward(summary, messageForward.signature());
        assertNotEquals(messageForward, notEqual);
        assertNotEquals(messageForward.hashCode(), notEqual.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSignatureMatching() {
        var b = new MessageForward(summary, Arrays.copyOf(messageForward.signature(), messageForward.signature().length));
        assertNotSame(messageForward.signature(), b.signature());
        assertNotEquals(messageForward.signature(), b.signature());
        assertArrayEquals(messageForward.signature(), b.signature());
        assertEquals(messageForward, b);
        assertEquals(messageForward.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDataNotMatching() {
        var sig = new byte[]{77};
        assertFalse(Arrays.equals(messageForward.signature(), sig));
        var notEqual = new MessageForward(summary, sig);
        assertNotEquals(messageForward, notEqual);
        assertNotEquals(messageForward.hashCode(), notEqual.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDifferentClass() {
        var b = new Object();
        assertNotEquals(messageForward, b);
        assertNotEquals(messageForward.hashCode(), b.hashCode());
    }

}