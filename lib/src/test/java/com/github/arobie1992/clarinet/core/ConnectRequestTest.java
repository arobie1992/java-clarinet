package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectRequestTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final PeerId sender = PeerUtils.senderId();
    private final ConnectionOptions options = new ConnectionOptions(PeerUtils.senderId());

    @Test
    void testNullConnectionId() {
        assertThrows(NullPointerException.class, () -> new ConnectRequest(null, sender, options));
    }

    @Test
    void testNullSender() {
        assertThrows(NullPointerException.class, () -> new ConnectRequest(connectionId, null, options));
    }

    @Test
    void testNullOptions() {
        assertThrows(NullPointerException.class, () -> new ConnectRequest(connectionId, sender, null));
    }

}