package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.transport.Transport;

public interface NodeBuilder {
    NodeBuilder id(PeerId id);
    NodeBuilder peerStore(PeerStore peerStore);
    NodeBuilder transport(Transport transport);
    Node build();
}
