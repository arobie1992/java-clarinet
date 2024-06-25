package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Objects;

public record ConnectionOptions(PeerId witnessSelector) {
    public ConnectionOptions {
        Objects.requireNonNull(witnessSelector);
    }
}
