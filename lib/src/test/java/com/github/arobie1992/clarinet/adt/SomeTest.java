package com.github.arobie1992.clarinet.adt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SomeTest {
    @Test
    void testNullNotPermitted() {
        var ex = assertThrows(NullPointerException.class, () -> new Some<>(null));
        assertEquals("value must not be null", ex.getMessage());
    }
}