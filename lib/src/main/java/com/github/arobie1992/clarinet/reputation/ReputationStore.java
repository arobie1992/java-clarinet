package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.stream.Stream;

public interface ReputationStore {
    void save(Reputation reputation);

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
