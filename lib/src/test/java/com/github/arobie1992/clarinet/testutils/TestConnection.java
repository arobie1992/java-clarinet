package com.github.arobie1992.clarinet.testutils;

import com.github.arobie1992.clarinet.core.Connection;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test-only class to allow for simplified verification of connections. Should mirror structure of
 * {@link Connection}.
 */
public record TestConnection(ConnectionId id, PeerId sender, Optional<PeerId> witness, PeerId receiver, Connection.Status status) {
    public void assertMatches(Connection connection) {
        assertEquals(id(), connection.id());
        assertEquals(sender(), connection.sender());
        assertEquals(witness(), connection.witness());
        assertEquals(receiver(), connection.receiver());
        assertEquals(status(), connection.status());
    }
}
