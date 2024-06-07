package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.VerificationException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class JavaSignatureSha256RsaPublicKey implements PublicKey {

    private final java.security.PublicKey javaKey;

    public JavaSignatureSha256RsaPublicKey(java.security.PublicKey javaKey) {
        this.javaKey = javaKey;
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        try {
            var verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(javaKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new VerificationException(e);
        }
    }

    @Override
    public String algorithm() {
        return "SHA256withRSA";
    }

    @Override
    public byte[] bytes() {
        return javaKey.getEncoded();
    }
}
