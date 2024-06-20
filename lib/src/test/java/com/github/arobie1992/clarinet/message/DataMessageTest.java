package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataMessageTest {

    private Bytes data;
    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);
    private DataMessage message;
    private Bytes senderSignature;

    @BeforeEach
    void setUp() {
        data = Bytes.of(new byte[]{0, 2, 4, 5, 6});
        message = new DataMessage(messageId, data);

        senderSignature = Bytes.of(new byte[]{9, 8});
        message.setSenderSignature(senderSignature);

        var witnessSignature = Bytes.of(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        message.setWitnessSignature(witnessSignature);
    }

    @Test
    void testSenderParts() {
        var senderParts = message.senderParts();
        assertEquals(new DataMessage.SenderParts(message.messageId(), message.data()), senderParts);
    }

    @Test
    void testWitnessParts() {
        var witnessParts = message.witnessParts();
        assertEquals(new DataMessage.WitnessParts(message.messageId(), message.data(), senderSignature), witnessParts);
    }

    @Test
    void testSenderSignatureNotSet() {
        message = new DataMessage(messageId, data);
        assertTrue(message.senderSignature().isEmpty());
    }

    @Test
    void testWitnessSignatureNotSet() {
        message = new DataMessage(messageId, data);
        assertTrue(message.witnessSignature().isEmpty());
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    void testEqualsAndHashCodeReflexive() {
        assertEquals(message, message);
    }

    @Test
    void testEqualsAndHashCodeSymmetric() {
        var matching = copy(message);
        assertEquals(message, matching);
        assertEquals(message.hashCode(), matching.hashCode());
        assertEquals(matching, message);
    }

    @Test
    void testEqualsAndHashCodeTransitive() {
        var matching1 = copy(message);
        var matching2 = copy(message);
        assertEquals(message, matching1);
        assertEquals(message.hashCode(), matching1.hashCode());

        assertEquals(matching1, matching2);
        assertEquals(matching1.hashCode(), matching2.hashCode());

        assertEquals(message, matching2);
        assertEquals(message.hashCode(), matching2.hashCode());
    }

    @Test
    void testEqualsAndHashCodeConsistent() {
        var matching = copy(message);
        assertEquals(message, matching);
        assertEquals(message.hashCode(), matching.hashCode());
        assertEquals(message, matching);
        assertEquals(message.hashCode(), matching.hashCode());
        assertEquals(message, matching);
        assertEquals(message.hashCode(), matching.hashCode());
    }

    @SuppressWarnings("SimplifiableAssertion")
    @Test
    void testEqualsNull() {
        // the point is to test what happens when null is passed and using assertNotNull was not exercising the equals method.
        //noinspection ConstantValue
        assertFalse(message.equals(null));
    }

    @Test
    void testEqualsDifferentClass() {
        assertNotEquals(message, new Object());
    }

    @Test
    void testEqualsMessageId() {
        var messageId = new MessageId(ConnectionId.random(), 0);
        assertNotEquals(messageId, message.messageId());
        var notEqual = new DataMessage(messageId, data);
        message.senderSignature().ifPresent(notEqual::setSenderSignature);
        message.witnessSignature().ifPresent(notEqual::setWitnessSignature);
        assertNotEquals(message, notEqual);
    }

    @Test
    void testEqualsData() {
        var data = Bytes.of(new byte[]{99});
        assertNotEquals(message.data(), data);
        var notEqual = new DataMessage(messageId, data);
        message.senderSignature().ifPresent(notEqual::setSenderSignature);
        message.witnessSignature().ifPresent(notEqual::setWitnessSignature);
        assertNotEquals(message, notEqual);
    }

    @Test
    void testEqualsSenderSignature() {
        var notEqual = new DataMessage(messageId, data);
        assertTrue(message.senderSignature().isPresent());
        assertTrue(notEqual.senderSignature().isEmpty());
        message.witnessSignature().ifPresent(notEqual::setWitnessSignature);
        assertNotEquals(message, notEqual);
    }

    @Test
    void testEqualsWitnessSignature() {
        var notEqual = new DataMessage(messageId, data);
        message.senderSignature().ifPresent(notEqual::setSenderSignature);
        assertTrue(message.witnessSignature().isPresent());
        assertTrue(notEqual.witnessSignature().isEmpty());
        assertNotEquals(message, notEqual);
    }

    private DataMessage copy(DataMessage message) {
        var copy = new DataMessage(messageId, data);
        message.senderSignature().ifPresent(copy::setSenderSignature);
        message.witnessSignature().ifPresent(copy::setWitnessSignature);
        return copy;
    }

    @Nested
    class SenderPartsTest {
        @Test
        void testEqualsAndHashCodeReflexive() {
            var a = message.senderParts();
            //noinspection EqualsWithItself
            assertEquals(a, a);
            assertEquals(a.hashCode(), a.hashCode());
        }

        @Test
        void testEqualsAndHashCodeSymmetric() {
            var a = message.senderParts();
            var b = message.senderParts();
            assertNotSame(a, b);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(b, a);
            assertEquals(b.hashCode(), a.hashCode());
        }

        @Test
        void testEqualsTransitive() {
            var a = message.senderParts();
            var b = message.senderParts();
            var c = message.senderParts();
            assertNotSame(a, b);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());

            assertNotSame(b, c);
            assertEquals(b, c);
            assertEquals(b.hashCode(), c.hashCode());

            assertNotSame(a, c);
            assertEquals(a, c);
            assertEquals(a.hashCode(), c.hashCode());
        }

        @Test
        void testEqualsConsistent() {
            var a = message.senderParts();
            var b = message.senderParts();
            assertNotSame(a, b);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void testEqualsNull() {
            // noinspection SimplifiableAssertion, ConstantValue
            assertFalse(message.senderParts().equals(null));
        }

        @Test
        void testEqualsMessageIdNotMatching() {
            var a = message.senderParts();
            var messageId = new MessageId(ConnectionId.random(), 0);
            assertNotEquals(a.messageId(), messageId);
            var notEqual = new DataMessage.SenderParts(messageId, data);
            assertNotEquals(a, notEqual);
            assertNotEquals(a.hashCode(), notEqual.hashCode());
        }

        @Test
        void testEqualsAndHashCodeDataNotMatching() {
            var a = message.senderParts();
            var data = Bytes.of(new byte[]{99});
            assertNotEquals(a.data(), data);
            var notEqual = new DataMessage.SenderParts(messageId, data);
            assertNotEquals(a, notEqual);
            assertNotEquals(a.hashCode(), notEqual.hashCode());
        }

        @Test
        void testEqualsAndHashCodeDifferentClass() {
            var a = message.senderParts();
            var b = new Object();
            assertNotEquals(a, b);
            assertNotEquals(a.hashCode(), b.hashCode());
        }
    }
}