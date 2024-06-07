package com.github.arobie1992.clarinet.crypto;

public non-sealed interface PrivateKeyProvider extends KeyProvider {
    @Override
    PrivateKey create(byte[] keyBytes);
}
