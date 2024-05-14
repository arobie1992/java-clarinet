package com.github.arobie1992.clarinet.peer;

public record ReadOnlyPeer(PeerId id, String address) implements Peer {
    public static Peer from(Peer peer) {
        return new ReadOnlyPeer(peer.id(), peer.address());
    }
}
