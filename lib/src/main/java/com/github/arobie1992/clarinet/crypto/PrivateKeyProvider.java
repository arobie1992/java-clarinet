package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;

public non-sealed interface PrivateKeyProvider extends KeyProvider {
    @Override
    PrivateKey create(Bytes keyBytes);
}
