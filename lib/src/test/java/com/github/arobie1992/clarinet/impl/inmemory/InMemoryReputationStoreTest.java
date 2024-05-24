package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.impl.reputation.ProportionalReputation;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryReputationStoreTest {

    private InMemoryReputationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryReputationStore(ProportionalReputation::new);
    }

    @Test
    void testFindAll() {
        var existing = new ProportionalReputation(PeerUtils.senderId());
        store.save(existing);

        var all = store.findAll(Stream.of(PeerUtils.senderId(), PeerUtils.receiverId())).collect(Collectors.toMap(Reputation::peerId, r -> r));
        assertEquals(2, all.size());

        var senderRep = verifyReputation(1, all.get(PeerUtils.senderId()));
        var receiverRep = verifyReputation(1, all.get(PeerUtils.receiverId()));

        senderRep.weakPenalize();
        store.save(senderRep);
        // don't save receiverRep so we can make sure it still has the unchanged value
        receiverRep.weakPenalize();

        all = store.findAll(Stream.of(PeerUtils.senderId(), PeerUtils.receiverId())).collect(Collectors.toMap(Reputation::peerId, r -> r));
        assertEquals(2, all.size());

        verifyReputation(0, all.get(PeerUtils.senderId()));
        verifyReputation(1, all.get(PeerUtils.receiverId()));
    }

    private Reputation verifyReputation(double expectedValue, Reputation reputation) {
        assertNotNull(reputation);
        assertEquals(expectedValue, reputation.value());
        return reputation;
    }
}