package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.transport.Transport;

import java.util.function.Supplier;

public interface NodeBuilder {
    NodeBuilder id(PeerId id);
    NodeBuilder peerStore(PeerStore peerStore);

    /**
     * Add the supplier to create the transport instance the node will use.
     * <p>
     * Because nodes may need to make modifications to the {@code Transport} to ensure protocol correctness,
     * this method accepts a {@code Supplier} that is subsequently used to instantiate the {@code Transport}
     * to discourage holding a reference to the transport independently of the node.
     * @param transportFactory A {@code Supplier} used to instantiate the {@code Transport} the node will use.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder transport(Supplier<Transport> transportFactory);
    Node build();
}
