package com.github.arobie1992.clarinet.peer;

import java.util.Optional;
import java.util.stream.Stream;

public interface PeerStore {
    /**
     * Save the given peer.
     * <p>
     * This operation adds the peer to this {@code PeerStore} if the peer is not already present. If the peer is
     * present, the current representation is replaced with the provided peer.
     * @param peer The {@code Peer} to save.
     */
    void save(Peer peer);

    Optional<Peer> find(PeerId id);

    /**
     * Retrieve a handle to access all the peers currently present.
     * <p>
     * This method returns a string to allow for lazy loading if retrieving peers is intensive.
     * Because retrieval is lazy, the backing view may change during the lifetime of the stream.
     * Implementations are strongly encouraged to make sure that mutations to the {@code Peer}s
     * returned from this stream are not persisted to the underlying storage unless they are saved.
     * @return A {@link Stream} of the {@link Peer}s.
     */
    Stream<PeerId> all();
}
