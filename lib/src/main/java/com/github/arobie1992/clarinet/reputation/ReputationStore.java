package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.stream.Stream;

public interface ReputationStore {
    void save(Reputation reputation);

    /**
     * Find the reputation corresponding to the given peer.
     * <p>
     * This function must return a reputation for the peer. If, for example, the backing store does not have a
     * reputation for the peer, then an implementation should provide a sane default.
     * @param peerId the peer to find reputations for
     * @return the reputations for the given peer.
     */
    Reputation find(PeerId peerId);

    /**
     * Find the reputations corresponding to all the given peers.
     * <p>
     * This function must return a reputation for all peers. If, for example, the backing store does not have a
     * reputation for any particular peer, then an implementation should provide a sane default.
     * @param peerIds the peers to find reputations for
     * @return a {@code Stream} of the reputations.
     */
    Stream<Reputation> findAll(Stream<PeerId> peerIds);
}
