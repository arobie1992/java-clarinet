package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class InMemoryReputationStore implements ReputationStore {
    private final Map<PeerId, Reputation> reputations = new HashMap<>();
    private final Function<PeerId, Reputation> defaultReputation;

    public InMemoryReputationStore(Function<PeerId, Reputation> defaultReputation) {
        this.defaultReputation = defaultReputation;
    }

    @Override
    public void save(Reputation reputation) {
        reputations.put(reputation.peerId(), reputation);
    }

    @Override
    public Stream<Reputation> findAll(Stream<PeerId> peerIds) {
        // Do a copy so mutations to the reputation don't persist unless saved
        return peerIds.map(peerId -> reputations.computeIfAbsent(peerId, defaultReputation).copy());
    }
}
