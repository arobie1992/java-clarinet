package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PeersRequestTest {

    @Test
    void testAdditionalRequestedBelowZero() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new PeersRequest(-1));
        assertEquals("additionalRequested must be non-negative", ex.getMessage());
    }

    @Test
    void testRequestedOnlyConstructor() {
        var peerRequest = new PeersRequest(Set.of(PeerUtils.witnessId()));
        assertEquals(0,  peerRequest.additionalRequested());
    }

    @Test
    void testNumOnlyConstructor() {
        var peerRequest = new PeersRequest(5);
        assertTrue(peerRequest.requested().isEmpty());
    }

}