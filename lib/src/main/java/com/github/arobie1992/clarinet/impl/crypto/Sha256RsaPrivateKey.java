package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.crypto.PrivateKey;
import com.github.arobie1992.clarinet.crypto.SigningException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

public class Sha256RsaPrivateKey implements PrivateKey {

    private final java.security.PrivateKey javaKey;

    public Sha256RsaPrivateKey(java.security.PrivateKey javaKey) {
        this.javaKey = javaKey;
    }

    @Override
    public Bytes sign(Bytes data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(data.bytes());
            var enc = digest.digest();
            var cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, javaKey);
            cipher.update(enc);
            return Bytes.of(cipher.doFinal());
        } catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|IllegalBlockSizeException|BadPaddingException e) {
            throw new SigningException(e);
        }
    }

    @Override
    public String algorithm() {
        return "SHA256withRSA";
    }

    @Override
    public Bytes bytes() {
        return Bytes.of(javaKey.getEncoded());
    }
}
