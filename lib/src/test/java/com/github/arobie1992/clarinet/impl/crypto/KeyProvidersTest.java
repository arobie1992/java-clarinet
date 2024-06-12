package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.crypto.KeyCreationException;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

class KeyProvidersTest {

    @Test
    void testJavaSignatureSha256RsaPublicKeyProvider() throws NoSuchAlgorithmException {
        var provider = KeyProviders.Sha256RsaPublicKeyProvider();
        assertTrue(provider.supports("SHA256withRSA"));
        var pair =  Keys.generateKeyPair();
        var createdKey = provider.create(pair.publicKey().bytes());
        var data = new byte[]{55};
        var sig = pair.privateKey().sign(data);
        assertTrue(createdKey.verify(data, sig));
    }

    @Test
    void testJavaSignatureSha256RsaPublicKeyProviderJunkBytes() {
        var provider = KeyProviders.Sha256RsaPublicKeyProvider();
        var ex = assertThrows(KeyCreationException.class, () -> provider.create(new byte[]{62}));
        assertNotNull(ex.getCause());
        assertEquals(InvalidKeySpecException.class, ex.getCause().getClass());
    }

}