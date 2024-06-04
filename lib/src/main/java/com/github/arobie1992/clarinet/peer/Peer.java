package com.github.arobie1992.clarinet.peer;

import java.util.HashSet;
import java.util.Set;

public record Peer(PeerId id, Set<Address> addresses) {
    public Peer(PeerId peerId) {
        this(peerId, new HashSet<>());
    }

    /**
     * The unique identifier of this node. This is the identifier other nodes use to identify this node.
     * @return The {@code PeerId} corresponding to this node's unique identifier.
     */
    public PeerId id() {
        return id;
    }

    /**
     * All available addresses at which this node can be reached.
     * <p>
     * Mutations to the returned set are reflected in the stored representation.
     * @return a {@code Set} containing an {@code Address} for each address at which this node can be reached.
     */
    public Set<Address> addresses() {
        return addresses;
    }
}
