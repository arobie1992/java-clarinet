package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.crypto.RawKey;

import java.util.List;

public record KeysResponse(List<RawKey> keys) {
}
