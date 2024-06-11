package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataMessageTest {

    private byte[] data;
    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);
    private DataMessage message;
    private byte[] senderSignature;
    private byte[] witnessSignature;

    @BeforeEach
    void setUp() {
        data = new byte[]{0, 2, 4, 5, 6};
        message = new DataMessage(messageId, data);

        senderSignature = new byte[]{9, 8};
        message.setSenderSignature(senderSignature);

        witnessSignature = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        message.setWitnessSignature(witnessSignature);
    }

    @Test
    void testDataCopiedIn() {
        assertArrayEquals(data, message.data());
        assertEquals(0, data[0]);
        data[0] = 55;
        assertEquals(0, message.data()[0]);
    }

    @Test
    void testDataCopiedOut() {
        var data = message.data();
        assertEquals(0, data[0]);
        data[0] = 55;
        assertEquals(0, message.data()[0]);
    }

    @Test
    void testSenderParts() {
        var senderParts = message.senderParts();
        assertEquals(message.messageId(), senderParts.messageId());
        assertArrayEquals(message.data(), senderParts.data());
        // verify mutations don't get persisted back
        senderParts.data()[0] = 99;
        assertEquals(0, message.data()[0]);
    }

    @Test
    void testWitnessParts() {
        var witnessParts = message.witnessParts();
        assertEquals(message.messageId(), witnessParts.messageId());
        assertArrayEquals(message.data(), witnessParts.data());
        assertArrayEquals(senderSignature, witnessParts.senderSignature());
        // verify mutations don't get persisted back
        witnessParts.data()[0] = 99;
        assertEquals(0, message.data()[0]);
        witnessParts.senderSignature()[1] = 0;
        assertEquals(8, message.senderSignature().orElseThrow()[1]);
    }

    @Test
    void testSenderSignatureCopiesIn() {
        assertArrayEquals(senderSignature, message.senderSignature().orElseThrow());
        assertEquals(9, senderSignature[0]);
        senderSignature[0] = 55;
        assertEquals(9, message.senderSignature().orElseThrow()[0]);
    }

    @Test
    void testSenderSignatureCopiedOut() {
        var senderSignature = message.senderSignature().orElseThrow();
        assertEquals(9, senderSignature[0]);
        senderSignature[0] = 55;
        assertEquals(9, message.senderSignature().orElseThrow()[0]);
    }

    @Test
    void testSenderSignatureNotSet() {
        message = new DataMessage(messageId, data);
        assertTrue(message.senderSignature().isEmpty());
    }


    @Test
    void testWitnessSignatureCopiesIn() {
        assertArrayEquals(witnessSignature, message.witnessSignature().orElseThrow());
        assertEquals(0, witnessSignature[0]);
        senderSignature[0] = 55;
        assertEquals(0, message.witnessSignature().orElseThrow()[0]);
    }

    @Test
    void testWitnessSignatureCopiedOut() {
        var witnessSignature = message.witnessSignature().orElseThrow();
        assertEquals(0, witnessSignature[0]);
        witnessSignature[0] = 55;
        assertEquals(0, message.witnessSignature().orElseThrow()[0]);
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
        var data = new byte[]{99};
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
        void testEqualsAndHashCodeDataMatching() {
            var a = message.senderParts();
            var b = message.senderParts();
            assertNotSame(a.data(), b.data());
            assertNotEquals(a.data(), b.data());
            assertArrayEquals(a.data(), b.data());
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void testEqualsAndHashCodeDataNotMatching() {
            var a = message.senderParts();
            var data = new byte[]{99};
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

    @Nested
    class WitnessPartsTest {
        @Test
        void testEqualsAndHashCodeReflexive() {
            var a = message.witnessParts();
            //noinspection EqualsWithItself
            assertEquals(a, a);
            assertEquals(a.hashCode(), a.hashCode());
        }

        @Test
        void testEqualsAndHashCodeSymmetric() {
            var a = message.witnessParts();
            var b = message.witnessParts();
            assertNotSame(a, b);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(b, a);
            assertEquals(b.hashCode(), a.hashCode());
        }

        @Test
        void testEqualsTransitive() {
            var a = message.witnessParts();
            var b = message.witnessParts();
            var c = message.witnessParts();
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
        void testEqualsAndHashCodeConsistent() {
            var a = message.witnessParts();
            var b = message.witnessParts();
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
            assertFalse(message.witnessParts().equals(null));
        }

        @Test
        void testEqualsAndHashCodeMessageIdNotMatching() {
            var a = message.witnessParts();
            var messageId = new MessageId(ConnectionId.random(), 0);
            assertNotEquals(a.messageId(), messageId);
            var notEqual = new DataMessage.WitnessParts(messageId, data, null);
            assertNotEquals(a, notEqual);
            assertNotEquals(a.hashCode(), messageId.hashCode());
        }

        @Test
        void testEqualsAndHashCodeDataMatching() {
            var a = message.witnessParts();
            var b = message.witnessParts();
            assertNotSame(a.data(), b.data());
            assertNotEquals(a.data(), b.data());
            assertArrayEquals(a.data(), b.data());
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void testEqualsDataNotMatching() {
            var a = message.witnessParts();
            var data = new byte[]{99};
            assertNotEquals(a.data(), data);
            var notEqual = new DataMessage.WitnessParts(messageId, data, null);
            assertNotEquals(a, notEqual);
            assertNotEquals(a.hashCode(), messageId.hashCode());
        }

        @Test
        void testEqualsAndHashCodeSenderSigMatching() {
            var a = new DataMessage.WitnessParts(messageId, data, new byte[]{17});
            var b = new DataMessage.WitnessParts(messageId, data, new byte[]{17});
            assertNotSame(a.senderSignature(), b.senderSignature());
            assertNotEquals(a.senderSignature(), b.senderSignature());
            assertArrayEquals(a.senderSignature(), b.senderSignature());
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void testEqualsAndHashCodeSenderSigNotMatching() {
            var a = new DataMessage.WitnessParts(messageId, data, new byte[]{17});
            var b = new DataMessage.WitnessParts(messageId, data, new byte[]{18});
            assertNotEquals(a, b);
            assertNotEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void testEqualsAndHashCodeDifferentClass() {
            var a = message.witnessParts();
            var other = new Object();
            assertNotEquals(a, other);
            assertNotEquals(a.hashCode(), other.hashCode());
        }
    }
}