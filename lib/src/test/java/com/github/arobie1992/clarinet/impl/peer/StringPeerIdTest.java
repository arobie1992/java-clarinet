package com.github.arobie1992.clarinet.impl.peer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringPeerIdTest {

    @Test
    void test() {
        var string = "test";
        var id = new StringPeerId(string);
        assertEquals(string, id.asString());
        assertEquals(id, id.parseFunction().apply(id.asString()));
    }

}