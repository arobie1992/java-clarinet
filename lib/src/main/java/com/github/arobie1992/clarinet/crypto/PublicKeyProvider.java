package com.github.arobie1992.clarinet.crypto;

public non-sealed interface PublicKeyProvider extends KeyProvider {
    @Override
    PublicKey create(byte[] keyBytes);
}
