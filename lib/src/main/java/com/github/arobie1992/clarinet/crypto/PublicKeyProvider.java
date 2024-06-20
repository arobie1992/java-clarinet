package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;

public non-sealed interface PublicKeyProvider extends KeyProvider {
    @Override
    PublicKey create(Bytes keyBytes);
}
