package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryResultTest {

    private final PeerId peerId = PeerUtils.senderId();
    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);
    private final QueryResponse queryResponse = new QueryResponse(null, null, null);

    @Test
    void testNullQueriedPeer() {
        assertThrows(NullPointerException.class, () -> new QueryResult(null, messageId, queryResponse));
    }

    @Test
    void testNullMessageId() {
        assertThrows(NullPointerException.class, () -> new QueryResult(peerId, null, queryResponse));
    }

    @Test
    void testNullQueryResponse() {
        assertThrows(NullPointerException.class, () -> new QueryResult(peerId, messageId, null));
    }
}