package com.github.arobie1992.clarinet.impl.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.VerificationException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Arrays;

public class Sha256RsaPublicKey implements PublicKey {

    private final java.security.PublicKey javaKey;

    public Sha256RsaPublicKey(java.security.PublicKey javaKey) {
        this.javaKey = javaKey;
    }

    @Override
    public boolean verify(Bytes data, Bytes signature) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(data.bytes());
            var enc = Bytes.of(digest.digest());
            return verifyHash(enc, signature);
        } catch (NoSuchAlgorithmException e) {
            throw new VerificationException(e);
        }
    }

    @Override
    public boolean verifyHash(Bytes hash, Bytes signature) {
        if(hash.bytes().length != 32) {
            throw new VerificationException("hash is not a valid SHA-256");
        }
        try {
            var cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, javaKey);
            cipher.update(signature.bytes());
            var dec = cipher.doFinal();
            return Arrays.equals(hash.bytes(), dec);
        } catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|IllegalBlockSizeException|BadPaddingException e) {
            throw new VerificationException(e);
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
