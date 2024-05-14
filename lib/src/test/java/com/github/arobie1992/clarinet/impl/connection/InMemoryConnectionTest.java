package com.github.arobie1992.clarinet.impl.connection;

import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionStatus;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.ReadOnlyPeer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryConnectionTest {

    private record TestPeerId() implements PeerId{}

    private final InMemoryConnection conn = new InMemoryConnection(
            ConnectionId.random(),
            ConnectionStatus.OPEN,
            new ReadOnlyPeer(new TestPeerId(), "sender"),
            null,
            new ReadOnlyPeer(new TestPeerId(), "receiver")
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
        var witness = new ReadOnlyPeer(new TestPeerId(), "witness");
        var updated = conn.setWitness(witness);
        assertEquals(witness, updated.witness().orElseThrow());
        assertTrue(conn.witness().isEmpty());

        assertEquals(conn.connectionId(), updated.connectionId());
        assertEquals(conn.status(), updated.status());
        assertEquals(conn.sender(), updated.sender());
        assertEquals(conn.receiver(), updated.receiver());
    }
}