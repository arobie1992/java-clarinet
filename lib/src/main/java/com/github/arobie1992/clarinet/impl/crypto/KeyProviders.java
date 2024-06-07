package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.crypto.KeyCreationException;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.PublicKeyProvider;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeyProviders {
    private KeyProviders() {}

    private static final PublicKeyProvider javaSignatureSha256RsaPublicKeyProvider = new PublicKeyProvider() {
        @Override
        public PublicKey create(byte[] keyBytes) {
            try {
                var kf = KeyFactory.getInstance("RSA");
                var publicKey = kf.generatePublic(new X509EncodedKeySpec(keyBytes));
                return new JavaSignatureSha256RsaPublicKey(publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new KeyCreationException(e);
            }
        }
        @Override
        public boolean supports(String algorithm) {
            return "SHA256withRSA".equals(algorithm);
        }
    };

    public static PublicKeyProvider javaSignatureSha256RsaPublicKeyProvider() {
        return javaSignatureSha256RsaPublicKeyProvider;
    }
}
