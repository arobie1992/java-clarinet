package com.github.arobie1992.clarinet.peer;

import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReadOnlyPeerTest {

    @Test
    void testFrom() {
        var peer1 = PeerUtils.sender();
        var peer2 = ReadOnlyPeer.from(peer1);
        assertEquals(peer1, peer2);
    }

}