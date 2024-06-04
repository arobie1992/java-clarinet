package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

public class PeersRequestException extends RuntimeException {
    private final PeerId peerId;

    public PeersRequestException(PeerId peerId) {
        super("Failed to request peers from " + peerId);
        this.peerId = peerId;
    }

    public PeerId peerId() {
        return peerId;
    }
}
