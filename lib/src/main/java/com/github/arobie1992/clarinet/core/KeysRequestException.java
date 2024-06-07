package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

public class KeysRequestException extends RuntimeException {
    private final PeerId peerId;
    public KeysRequestException(PeerId peerId) {
        super("Failed to request keys from " + peerId);
        this.peerId = peerId;
    }
    public PeerId peerId() {
        return peerId;
    }
}
