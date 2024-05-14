package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.ReadOnlyPeer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadOnlyConnectionTest {

    private record TestPeerId() implements PeerId {}

    private final Connection conn = new ReadOnlyConnection(
            ConnectionId.random(),
            ConnectionStatus.OPEN,
            new ReadOnlyPeer(new TestPeerId(), "sender"),
            Optional.of(new ReadOnlyPeer(new TestPeerId(), "witness")),
            new ReadOnlyPeer(new TestPeerId(), "receiver")
    );

    @Test
    void testFrom() {
        var copy = ReadOnlyConnection.from(conn);
        assertEquals(conn.connectionId(), copy.connectionId());
        assertEquals(conn.status(), copy.status());
        assertEquals(conn.sender(), copy.sender());
        assertEquals(conn.witness(), copy.witness());
        assertEquals(conn.receiver(), copy.receiver());
    }

    @Test
    void testUpdateStatus() {
        assertThrows(UnsupportedOperationException.class, () -> conn.updateStatus(ConnectionStatus.NOTIFYING_OF_WITNESS));
    }

    @Test
    void testSetWitness() {
        assertThrows(UnsupportedOperationException.class, () -> conn.setWitness(null));
    }
}
