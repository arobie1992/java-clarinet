package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PeersRequestExceptionTest {

    private final PeerId peerId = PeerUtils.receiverId();
    private final PeersRequestException exception = new PeersRequestException(peerId);

    @Test
    void testMessage() {
        assertEquals("Failed to request peers from " + peerId, exception.getMessage());
    }

    @Test
    void testPeerId() {
        assertEquals(peerId, exception.peerId());
    }
}