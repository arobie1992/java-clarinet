package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.adt.Bytes;

public record RawKey(String algorithm, Bytes bytes) implements Key {
}
