package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

public class PeerSendException extends RuntimeException {
    private final PeerId peerId;

    public PeerSendException(PeerId peerId) {
        super("Failed to reach peer " + peerId + " at any addresses");
        this.peerId = peerId;
    }

    public PeerId peerId() {
        return peerId;
    }
}
