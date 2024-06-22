package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryForwardTest {

    private final PeerId peerId = PeerUtils.senderId();
    private final MessageDetails details = new MessageDetails(new MessageId(ConnectionId.random(), 0), Bytes.of(new byte[]{1}));
    private final QueryResponse queryResponse = new QueryResponse(details, Bytes.of(new byte[]{2}), "SHA-256");
    private final Bytes sig = Bytes.of(new byte[]{3,4,2,1});

    @Test
    void testNullPeerId() {
        assertThrows(NullPointerException.class, () -> new QueryForward(null, queryResponse, sig));
    }

    @Test
    void testNullQueryResponse() {
        assertThrows(NullPointerException.class, () -> new QueryForward(peerId, null, sig));
    }

    @Test
    void testNullSignature() {
        assertThrows(NullPointerException.class, () -> new QueryForward(peerId, queryResponse, null));
    }
}