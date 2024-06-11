package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloseOptionsTest {

    @Test
    void testNoArgsConstructor() {
        CloseOptions closeOptions = new CloseOptions();
        assertTrue(closeOptions.connectionObtainTimeout().isEmpty());
    }

}