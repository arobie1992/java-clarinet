package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.Set;

public interface Node {
    PeerId id();
    Set<Address> addresses();
    PeerStore peerStore();
    Connection.ReadableReference findConnection(ConnectionId connectionId);
    /**
     * Establishes an outgoing communication channel with the specified node at the specified address.
     * <p>
     * If the node does not have an already-existing peer corresponding to {@code peerId}, it adds a peer with this ID.
     * If the peer does not have the specified address, the peer is updated to include this address. The returned
     * {@code ConnectionId} must always be non-null; if an implementation wishes to signal failures, it should do so
     * through throwing exceptions.
     *
     * @param receiver The peer ID of the receiver and the address at which it will be contacted for this connection.
     * @param connectionOptions Any connection configuration. See {@link ConnectionOptions} and specific implementation
     *                          notes for supported options.
     * @param transportOptions Any configuration used for the underlying transport protocol. See {@link TransportOptions}
     *                         and specific implementation notes for supported options.
     * @return the {@link ConnectionId} of the newly created connection.
     */
    ConnectionId connect(PeerId receiver, ConnectionOptions connectionOptions, TransportOptions transportOptions);
}
