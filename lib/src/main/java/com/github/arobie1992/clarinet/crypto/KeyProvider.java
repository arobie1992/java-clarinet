package com.github.arobie1992.clarinet.crypto;

public sealed interface KeyProvider permits PrivateKeyProvider, PublicKeyProvider {
    Key create(byte[] keyBytes);
    boolean supports(String algorithm);
}
