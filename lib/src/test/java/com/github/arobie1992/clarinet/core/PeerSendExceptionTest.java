package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PeerSendExceptionTest {

    private final PeerId peerId = PeerUtils.senderId();
    private final PeerSendException exception = new PeerSendException(peerId);

    @Test
    void testMessage() {
        assertEquals("Failed to reach peer " + peerId + " at any addresses", exception.getMessage());
    }

    @Test
    void testPeerId() {
        assertEquals(peerId, exception.peerId());
    }

}