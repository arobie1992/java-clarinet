package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;

public sealed interface KeyProvider permits PrivateKeyProvider, PublicKeyProvider {
    Key create(Bytes keyBytes);
    boolean supports(String algorithm);
}
