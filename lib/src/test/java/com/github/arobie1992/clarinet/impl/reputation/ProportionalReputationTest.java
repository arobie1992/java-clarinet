package com.github.arobie1992.clarinet.impl.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProportionalReputationTest {

    private final PeerId peerId = PeerUtils.senderId();
    private Reputation reputation;

    @BeforeEach
    void setUp() {
        reputation = new ProportionalReputation(peerId);
    }

    @Test
    void testPeerId() {
        assertEquals(peerId, reputation.peerId());
    }

    @Test
    void testReputationOperations() {
        assertEquals(1, reputation.value());
        reputation.weakPenalize();
        assertEquals(0, reputation.value());
        reputation.reward();
        assertEquals(0.5, reputation.value());
        reputation.strongPenalize();
        assertEquals(0.2, reputation.value());
    }

    @Test
    void testCopy() {
        var copy = reputation.copy();
        assertEquals(reputation.value(), copy.value());
        copy.weakPenalize();
        assertEquals(0, copy.value());
        assertEquals(1, reputation.value());
    }

}