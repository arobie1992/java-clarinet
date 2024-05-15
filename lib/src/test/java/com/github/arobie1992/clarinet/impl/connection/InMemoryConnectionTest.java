package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionStatus;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryConnectionTest {

    private final InMemoryConnection conn = new InMemoryConnection(
            ConnectionId.random(),
            ConnectionStatus.OPEN,
            PeerUtils.sender(),
            null,
            PeerUtils.receiver()
    );

    @Test
    void testUpdateStatus() {
        var updated = conn.updateStatus(ConnectionStatus.CLOSED);
        assertEquals(ConnectionStatus.CLOSED, updated.status());
        assertEquals(ConnectionStatus.OPEN, conn.status());

        assertEquals(conn.connectionId(), updated.connectionId());
        assertEquals(conn.sender(), updated.sender());
        assertEquals(conn.witness(), updated.witness());
        assertEquals(conn.receiver(), updated.receiver());
    }

    @Test
    void testSetWitness() {
        var witness = PeerUtils.witness();
        var updated = conn.setWitness(witness);
        assertEquals(witness, updated.witness().orElseThrow());
        assertTrue(conn.witness().isEmpty());

        assertEquals(conn.connectionId(), updated.connectionId());
        assertEquals(conn.status(), updated.status());
        assertEquals(conn.sender(), updated.sender());
        assertEquals(conn.receiver(), updated.receiver());
    }
}