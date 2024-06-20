package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;

public interface PrivateKey extends Key {
    Bytes sign(Bytes data);
}
