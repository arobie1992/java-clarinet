package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

public class NoSuchPeerException extends RuntimeException {
    private final PeerId peerId;

    public NoSuchPeerException(PeerId peerId) {
        super("No such peer: " + peerId);
        this.peerId = peerId;
    }

    public PeerId peerId() {
        return peerId;
    }
}
