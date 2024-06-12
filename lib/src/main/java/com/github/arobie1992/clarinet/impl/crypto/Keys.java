package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.crypto.KeyPair;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Keys {
    private Keys() {}

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048, new SecureRandom());
        var keyPair = gen.generateKeyPair();
        return new KeyPair(
                new Sha256RsaPublicKey(keyPair.getPublic()),
                new Sha256RsaPrivateKey(keyPair.getPrivate())
        );
    }
}
