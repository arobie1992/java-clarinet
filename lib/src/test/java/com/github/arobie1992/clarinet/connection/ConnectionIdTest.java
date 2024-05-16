package com.github.arobie1992.clarinet.connection;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionIdTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final ConnectionId matching = new ConnectionId(UUID.fromString(connectionId.toString()));

    @Test
    void testEqualsAndHashCodeReflexive() {
        assertEquals(connectionId, connectionId);
        assertEquals(connectionId.hashCode(), connectionId.hashCode());
    }

    @Test
    void testEqualsAndHashCodeSymmetric() {
        assertEquals(connectionId, matching);
        assertEquals(matching, connectionId);
        assertEquals(connectionId.hashCode(), matching.hashCode());
    }

    @Test
    void testEqualsAndHashCodeTransitive() {
        var matching2 = new ConnectionId(UUID.fromString(connectionId.toString()));

        assertEquals(connectionId, matching);
        assertEquals(matching, matching2);
        assertEquals(connectionId, matching);

        assertEquals(connectionId.hashCode(), matching.hashCode());
        assertEquals(matching.hashCode(), matching2.hashCode());
        assertEquals(connectionId.hashCode(), matching2.hashCode());
    }

    @Test
    void testEqualsAndHashCodeConsistentTrue() {
        assertEquals(connectionId, matching);
        assertEquals(connectionId, matching);
        assertEquals(connectionId, matching);
    }

    @Test
    void testEqualsAndHashCodeConsistentFalse() {
        var notMatching = ConnectionId.random();
        assertNotEquals(connectionId, notMatching);
        assertNotEquals(connectionId, notMatching);
        assertNotEquals(connectionId, notMatching);
    }

    @Test
    void testEqualsNull() {
        assertNotEquals(connectionId, null);
    }

    @Test
    void testEqualsDifferentClass() {
        var other = new Object();
        assertNotEquals(connectionId, other);
        assertNotEquals(connectionId.hashCode(), other.hashCode());
    }

}