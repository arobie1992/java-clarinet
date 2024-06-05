package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.BeforeEach;
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
}