package com.github.arobie1992.clarinet.impl.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.adt.Bytes;
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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

// Test the pub and private keys together to make sure sign and verify work
@TestInstance(PER_CLASS)
class Sha256RsaKeyTest {

    private final java.security.KeyPair javaPair;
    private final KeyPair keyPair;
    private final Bytes data = Bytes.of(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    private final ObjectMapper objectMapper = new ObjectMapper();

    Sha256RsaKeyTest() throws NoSuchAlgorithmException {
        var module = new SimpleModule();
        module.addSerializer(PeerId.class, new PeerIdSerializer());
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());

        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048, new SecureRandom());
        javaPair = gen.generateKeyPair();
        keyPair = new KeyPair(
                new Sha256RsaPublicKey(javaPair.getPublic()),
                new Sha256RsaPrivateKey(javaPair.getPrivate())
        );
    }

    @Test
    void testSignAndVerify() {
        var sig = keyPair.privateKey().sign(data);
        assertTrue(keyPair.publicKey().verify(data, sig));
    }

    @Test
    void testSignAndVerifyDataMessage() throws JsonProcessingException {
        var message = new DataMessage(new MessageId(ConnectionId.random(), 0), data);
        var serialized = Bytes.of(objectMapper.writeValueAsBytes(message.senderParts()));
        var sig = keyPair.privateKey().sign(serialized);
        message.setSenderSignature(sig);
        assertTrue(keyPair.publicKey().verify(serialized, message.senderSignature().orElseThrow()));
    }

    @Test
    void testSignThrowsNoSuchAlgorithmException() {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            cipher.when(() -> Cipher.getInstance("RSA")).thenThrow(NoSuchAlgorithmException.class);
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(NoSuchAlgorithmException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testSignThrowsNoSuchPaddingException() {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            cipher.when(() -> Cipher.getInstance("RSA")).thenThrow(NoSuchPaddingException.class);
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(NoSuchPaddingException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testSignThrowsInvalidKeyException() throws InvalidKeyException {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            var cipherMock = Mockito.mock(Cipher.class);
            cipher.when(() -> Cipher.getInstance("RSA")).thenReturn(cipherMock);
            doThrow(InvalidKeyException.class).when(cipherMock).init(eq(Cipher.ENCRYPT_MODE), any(Key.class));
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(InvalidKeyException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testSignThrowsIllegalBlockSizeException() throws IllegalBlockSizeException, BadPaddingException {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            var cipherMock = Mockito.mock(Cipher.class);
            cipher.when(() -> Cipher.getInstance("RSA")).thenReturn(cipherMock);
            doThrow(IllegalBlockSizeException.class).when(cipherMock).doFinal();
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(IllegalBlockSizeException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testSignThrowsBadPaddingException() throws IllegalBlockSizeException, BadPaddingException {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            var cipherMock = Mockito.mock(Cipher.class);
            cipher.when(() -> Cipher.getInstance("RSA")).thenReturn(cipherMock);
            doThrow(BadPaddingException.class).when(cipherMock).doFinal();
            var ex = assertThrows(SigningException.class, () -> keyPair.privateKey().sign(data));
            assertEquals(BadPaddingException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyThrowsNoSuchAlgorithmException() {
        try(var md = Mockito.mockStatic(MessageDigest.class)) {
            md.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(NoSuchAlgorithmException.class);
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, Bytes.of(new byte[]{0})));
            assertEquals(NoSuchAlgorithmException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyHashThrowsNoSuchAlgorithmException() {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            cipher.when(() -> Cipher.getInstance("RSA")).thenThrow(NoSuchAlgorithmException.class);
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, Bytes.of(new byte[]{0})));
            assertEquals(NoSuchAlgorithmException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyHashThrowsNoSuchPaddingException() {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            cipher.when(() -> Cipher.getInstance("RSA")).thenThrow(NoSuchPaddingException.class);
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, Bytes.of(new byte[]{0})));
            assertEquals(NoSuchPaddingException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyHashThrowsInvalidKeyException() throws InvalidKeyException {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            var cipherMock = Mockito.mock(Cipher.class);
            cipher.when(() -> Cipher.getInstance("RSA")).thenReturn(cipherMock);
            doThrow(InvalidKeyException.class).when(cipherMock).init(eq(Cipher.DECRYPT_MODE), any(Key.class));
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, Bytes.of(new byte[]{0})));
            assertEquals(InvalidKeyException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyHashThrowsIllegalBlockSizeException() throws IllegalBlockSizeException, BadPaddingException {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            var cipherMock = Mockito.mock(Cipher.class);
            cipher.when(() -> Cipher.getInstance("RSA")).thenReturn(cipherMock);
            doThrow(IllegalBlockSizeException.class).when(cipherMock).doFinal();
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, Bytes.of(new byte[]{0})));
            assertEquals(IllegalBlockSizeException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyHashThrowsBadPaddingException() throws IllegalBlockSizeException, BadPaddingException {
        try(var cipher = Mockito.mockStatic(Cipher.class)) {
            var cipherMock = Mockito.mock(Cipher.class);
            cipher.when(() -> Cipher.getInstance("RSA")).thenReturn(cipherMock);
            doThrow(BadPaddingException.class).when(cipherMock).doFinal();
            // sig doesn't matter so just pass dummy data
            var ex = assertThrows(VerificationException.class, () -> keyPair.publicKey().verify(data, Bytes.of(new byte[]{0})));
            assertEquals(BadPaddingException.class, ex.getCause().getClass());
        }
    }

    @Test
    void testVerifyHashNot32Bytes() {
        var ex = assertThrows(
                VerificationException.class,
                () -> keyPair.publicKey().verifyHash(Bytes.of(new byte[]{23}), Bytes.of(new byte[]{0}))
        );
        assertEquals("hash is not a valid SHA-256", ex.getMessage());
    }

    @Test
    void testVerifyHash() throws JsonProcessingException, NoSuchAlgorithmException {
        var message = new DataMessage(new MessageId(ConnectionId.random(), 0), data);
        var serialized = Bytes.of(objectMapper.writeValueAsBytes(message.senderParts()));
        var sig = keyPair.privateKey().sign(serialized);
        var digest = MessageDigest.getInstance("SHA-256");
        var hash = digest.digest(serialized.bytes());
        assertTrue(keyPair.publicKey().verifyHash(Bytes.of(hash), sig));
    }

    @ParameterizedTest
    @MethodSource("algsKeys")
    void testAlgorithm(com.github.arobie1992.clarinet.crypto.Key key, String alg) {
        assertEquals(alg, key.algorithm());
    }

    private Stream<Arguments> algsKeys() {
        return Stream.of(
                Arguments.of(keyPair.publicKey(), "SHA256withRSA"),
                Arguments.of(keyPair.privateKey(), "SHA256withRSA")
        );
    }

    @ParameterizedTest
    @MethodSource("bytesKeys")
    void testBytes(com.github.arobie1992.clarinet.crypto.Key key, Bytes expected) {
        assertEquals(expected, key.bytes());
    }

    private Stream<Arguments> bytesKeys() {
        return Stream.of(
                Arguments.of(keyPair.publicKey(), Bytes.of(javaPair.getPublic().getEncoded())),
                Arguments.of(keyPair.privateKey(), Bytes.of(javaPair.getPrivate().getEncoded()))
        );
    }
    
}