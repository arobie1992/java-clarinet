package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoSuchPeerExceptionTest {

    private final NoSuchPeerException exception = new NoSuchPeerException(PeerUtils.senderId());

    @Test
    void testMessage() {
        assertEquals("No such peer: " + PeerUtils.senderId(), exception.getMessage());
    }

    @Test
    void testPeerId() {
        assertEquals(PeerUtils.senderId(), exception.peerId());
    }
}