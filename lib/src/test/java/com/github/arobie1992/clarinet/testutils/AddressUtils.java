package com.github.arobie1992.clarinet.testutils;

import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.UncheckedURISyntaxException;

import java.net.URI;
import java.net.URISyntaxException;

public class AddressUtils {
    private AddressUtils() {}

    public static Address defaultAddress() {
        try {
            return new UriAddress(new URI("tcp://localhost"));
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        }
    }
}
