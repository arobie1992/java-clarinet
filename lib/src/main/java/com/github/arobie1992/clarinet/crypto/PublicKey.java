package com.github.arobie1992.clarinet.crypto;

public interface PublicKey extends Key {
    boolean verify(byte[] data, byte[] signature);

    /**
     * Equivalent to {@link PublicKey#verify(byte[], byte[])} except the data is already hashed.
     * <p>
     * In a cryptographic signature, the signature is defined as the ciphertext of a hash of the data using the private
     * key, `sig(x) = priv.enc(hash(x))`. Verification consists of comparing the plaintext of the signature with the
     * hash of the data, `verify(x, sig) = hash(x) == pub.dec(sig)`. This is what{@link PublicKey#verify(byte[], byte[])}
     * does. However, sometimes, it is beneficial to send just the hash rather than the entire message `x`. In these
     * cases `verify` will not work because `hash(hash(x)) != pub.dec(sig)`. This method is to support such cases, and
     * so does not perform the hashing as part of the verification, `verifyHash(x, sig) = x == pub.dec(sig)`.
     * @param hash the hash of the data against which the signature is verified.
     * @param signature the cryptographic signature of the data to be verified.
     * @return {@code true} if the {@code signature} is valid for the given {@code hash}, {@code false} otherwise.
     */
    boolean verifyHash(byte[] hash, byte[] signature);
}
