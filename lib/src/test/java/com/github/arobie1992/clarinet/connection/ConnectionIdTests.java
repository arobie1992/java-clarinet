package com.github.arobie1992.clarinet.connection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ConnectionIdTests {

    private final ConnectionId connectionId = ConnectionId.random();

    @Test
    void testToString() {
        var str = connectionId.toString();
        assertDoesNotThrow(() -> UUID.fromString(str));
    }

    @Test
    void testEqualsAndHashCodeSelf() {
        assertEquals(connectionId, connectionId);
        assertEquals(connectionId.hashCode(), connectionId.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDiffClass() {
        var str = "test";
        assertNotEquals(connectionId, str);
        assertNotEquals(connectionId.hashCode(), str.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDiffObjSameVal() {
        var other = new ConnectionId(UUID.fromString(connectionId.toString()));
        assertEquals(connectionId, other);
        assertEquals(connectionId.hashCode(), other.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDiffObjDifferentVal() {
        var other = ConnectionId.random();
        assertNotEquals(connectionId, other);
        assertNotEquals(connectionId.hashCode(), other.hashCode());
    }

    @Test
    void testEqualsNull() {
        assertNotEquals(connectionId, null);
    }
}
