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

}