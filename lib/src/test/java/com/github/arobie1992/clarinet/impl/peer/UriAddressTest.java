package com.github.arobie1992.clarinet.impl.peer;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class UriAddressTest {

    @Test
    void test() throws URISyntaxException {
        var uri = new URI("tcp://localhost");
        var address = new UriAddress(uri);
        assertEquals(uri, address.asURI());
        assertEquals(address, address.parseFunction().apply(address.asURI()));
    }

}