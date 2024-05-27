package com.github.arobie1992.clarinet.impl.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.crypto.KeyPair;
import com.github.arobie1992.clarinet.crypto.SigningException;
import com.github.arobie1992.clarinet.crypto.VerificationException;
import com.github.arobie1992.clarinet.impl.netty.ConnectionIdSerializer;
import com.github.arobie1992.clarinet.impl.netty.PeerIdSerializer;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

// Test the pub and private keys together to make sure sign and verify work
class JavaSignatureSha256RsaKeyTest {

    private final KeyPair keyPair = Keys.generateKeyPair();
    private final byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private final ObjectMapper objectMapper = new ObjectMapper();

    JavaSignatureSha256RsaKeyTest() throws NoSuchAlgorithmException {
        var module = new SimpleModule();
        module.addSerializer(PeerId.class, new PeerIdSerializer());
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());
    }

    @Test
    void testSignAndVerify() {
        var sig = keyPair.privateKey().sign(data);
        assertTrue(keyPair.publicKey().verify(data, sig));
    }

    @Test
    void testSignAndVerifyDataMessage() throws JsonProcessingException {
        var message = new DataMessage(new MessageId(ConnectionId.random(), 0), data);
        var serialized = objectMapper.writeValueAsBytes(message.senderParts());
        var sig = keyPair.privateKey().sign(serialized);
        message.setSenderSignature(sig);
        assertTrue(keyPair.publicKey().verify(serialized, message.senderSignature().orElseThrow()));
    }

    @Test
    void testSignThrowsNoSuchAlgorithmException() {
        try(var sig = Mockito.mockStatic(Signature.class)) {
            sig.when(() -> Signature.getInstance("SHA256withRSA")).thenThrow(NoSuchAlgorithmException.class);
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(NoSuchAlgorithmException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyThrowsNoSuchAlgorithmException() {
        try(var sig = Mockito.mockStatic(Signature.class)) {
            sig.when(() -> Signature.getInstance("SHA256withRSA")).thenThrow(NoSuchAlgorithmException.class);
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, new byte[]{0}));
            assertEquals(NoSuchAlgorithmException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testSignThrowsInvalidKeyException() throws InvalidKeyException {
        try(var sig = Mockito.mockStatic(Signature.class)) {
            var sigMock = Mockito.mock(Signature.class);
            sig.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(sigMock);
            doThrow(InvalidKeyException.class).when(sigMock).initSign(any());
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(InvalidKeyException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyThrowsInvalidKeyException() throws InvalidKeyException {
        try(var sig = Mockito.mockStatic(Signature.class)) {
            var sigMock = Mockito.mock(Signature.class);
            sig.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(sigMock);
            doThrow(InvalidKeyException.class).when(sigMock).initVerify(any(PublicKey.class));
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, new byte[]{0}));
            assertEquals(InvalidKeyException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testSignThrowsSignatureException() throws SignatureException {
        try(var sig = Mockito.mockStatic(Signature.class)) {
            var sigMock = Mockito.mock(Signature.class);
            sig.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(sigMock);
            doThrow(SignatureException.class).when(sigMock).update(data);
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(SignatureException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyThrowsSignatureException() throws SignatureException {
        try(var sig = Mockito.mockStatic(Signature.class)) {
            var sigMock = Mockito.mock(Signature.class);
            sig.when(() -> Signature.getInstance("SHA256withRSA")).thenReturn(sigMock);
            doThrow(SignatureException.class).when(sigMock).update(data);
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, new byte[]{0}));
            assertEquals(SignatureException.class, ex.getCause().getClass());
        }
    }

}