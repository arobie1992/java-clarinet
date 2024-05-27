package com.github.arobie1992.clarinet.crypto;

public interface PublicKey extends Key {
    boolean verify(byte[] data, byte[] signature);
}
