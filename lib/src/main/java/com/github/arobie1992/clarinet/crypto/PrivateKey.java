package com.github.arobie1992.clarinet.crypto;

public interface PrivateKey extends Key {
    byte[] sign(byte[] data);
}
