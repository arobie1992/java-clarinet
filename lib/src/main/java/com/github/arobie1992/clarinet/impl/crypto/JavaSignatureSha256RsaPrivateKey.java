package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.crypto.PrivateKey;
import com.github.arobie1992.clarinet.crypto.SigningException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

public class JavaSignatureSha256RsaPrivateKey implements PrivateKey {

    private final java.security.PrivateKey javaKey;

    public JavaSignatureSha256RsaPrivateKey(java.security.PrivateKey javaKey) {
        this.javaKey = javaKey;
    }

    @Override
    public byte[] sign(byte[] data) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(javaKey);
            signer.update(data);
            return signer.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new SigningException(e);
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
