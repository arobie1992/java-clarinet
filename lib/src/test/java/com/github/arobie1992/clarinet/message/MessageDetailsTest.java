package com.github.arobie1992.clarinet.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageDetailsTest {

    @Test
    void testNullMessageId() {
        assertThrows(NullPointerException.class, () -> new MessageDetails(null, null));
    }

}