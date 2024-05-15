package com.github.arobie1992.clarinet.impl.peer;

import com.github.arobie1992.clarinet.peer.PeerId;

public class ExistingPeerIdException extends RuntimeException {
    private final PeerId peerId;

    public ExistingPeerIdException(PeerId peerId) {
        super(String.format("The PeerId %s already exists", peerId));
        this.peerId = peerId;
    }

    public PeerId peerId() {
        return peerId;
    }
}
