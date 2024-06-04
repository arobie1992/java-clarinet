package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PeersRequestTest {

    @Test
    void testNumBelowZero() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new PeersRequest(-1));
        assertEquals("num must be greater than 0", ex.getMessage());
    }

    @Test
    void testNumIsZero() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new PeersRequest(0));
        assertEquals("num must be greater than 0", ex.getMessage());
    }

    @Test
    void testRequestedLargerThanNum() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> new PeersRequest(1, List.of(PeerUtils.witnessId(), PeerUtils.witnessId()))
        );
        assertEquals("requested cannot exceed num", ex.getMessage());
    }

    @Test
    void testRequestedOnlyConstructor() {
        var peerRequest = new PeersRequest(List.of(PeerUtils.witnessId()));
        assertEquals(1, peerRequest.num());
    }

    @Test
    void testNumOnlyConstructor() {
        var peerRequest = new PeersRequest(5);
        assertTrue(peerRequest.requested().isEmpty());
    }

}