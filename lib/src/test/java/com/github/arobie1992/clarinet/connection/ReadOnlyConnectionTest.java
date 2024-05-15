package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadOnlyConnectionTest {

    private final Connection conn = new ReadOnlyConnection(
            ConnectionId.random(),
            ConnectionStatus.OPEN,
            PeerUtils.sender(),
            Optional.of(PeerUtils.witness()),
            PeerUtils.receiver()
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
