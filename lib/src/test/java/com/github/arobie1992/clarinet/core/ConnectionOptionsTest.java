package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionOptionsTest {

    @Test
    void testNullWitnessSelector() {
        assertThrows(NullPointerException.class, () -> new ConnectionOptions(null));
    }

}