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
    void testFind() {
        var existing = new ProportionalReputation(PeerUtils.senderId());
        assertEquals(1, existing.value());

        store.save(existing);
        // verify copy in behavior
        existing.weakPenalize();
        assertEquals(0, existing.value());
        var retrieved = store.find(existing.peerId());
        assertEquals(1, retrieved.value());

        // verify copy out behavior
        retrieved.weakPenalize();
        assertEquals(0, retrieved.value());

        var retrieved2 = store.find(existing.peerId());
        assertEquals(1, retrieved2.value());

        // verify save behavior
        store.save(retrieved);
        retrieved2 = store.find(existing.peerId());
        assertEquals(0, retrieved2.value());
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