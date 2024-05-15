package com.github.arobie1992.clarinet.impl.peer;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.TestPeerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExistingPeerIdExceptionTest {

    private final PeerId peerId = new TestPeerId();
    private final ExistingPeerIdException exception = new ExistingPeerIdException(peerId);

    @Test
    void testMessage() {
        assertEquals(String.format("The PeerId %s already exists", peerId), exception.getMessage());
    }

    @Test
    void testPeerId() {
        assertEquals(peerId, exception.peerId());
    }

}