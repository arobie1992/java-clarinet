package com.github.arobie1992.clarinet.peer;

import java.util.Collection;
import java.util.function.Consumer;

public interface PeerStore {
    void read(PeerId peerId, Consumer<Peer> consumer);
    Collection<PeerId> all();
    PeerId self();
}
