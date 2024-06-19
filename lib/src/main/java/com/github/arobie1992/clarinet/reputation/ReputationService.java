package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

public interface ReputationService {
    /**
     * Update the reputation that the provided {@code Assessment}s correspond to {@link Assessment.Status} priorities.
     * <p>
     * More formally, if {@code existing == null} or {@code updated.status().comparePriority(existing.status()) > 0} the
     * reputation update must be reflected in subsequent calls to {@link ReputationService#get(PeerId)}. If
     * {@code updated.status().comparePriority(existing.status()) < 0}, the reputation
     * update must not be reflected in subsequent calls to {@link ReputationService#get(PeerId)}. If
     * {@code updated.status().comparePriority(existing.status()) == 0} implementations may
     * choose how to proceed so long as subsequent calls to {@link ReputationService#get(PeerId)} are not impacted. This
     * refers only to the reputation score returned from {@link ReputationService#get(PeerId)} for the {@code peerId}
     * the assessments refer to. Implementations are free to record other data, such as for bookkeeping, so long as it
     * does not affect the reputation score.
     * @param existing the assessment prior to being updated.
     * @param updated the assessment with its updated {@code status}.
     */
    default void update(Assessment existing, Assessment updated) {}

    /**
     * Retrieve the reputation for the provided {@code peerId}.
     * <p>
     * Reputation values must be between 0 and 1 inclusive.
     * @param peerId the unique identifier for the peer this reputation corresponds to.
     * @return a {@code double} between 0 and 1 inclusive representing the reputation score.
     */
    double get(PeerId peerId);
}
