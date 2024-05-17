package com.github.arobie1992.clarinet.impl.peer;

import com.github.arobie1992.clarinet.peer.Address;

import java.net.URI;
import java.util.function.Function;

public record UriAddress(URI uri) implements Address {
    @Override
    public URI asURI() {
        return uri;
    }

    @Override
    public Function<URI, Address> parseFunction() {
        return UriAddress::new;
    }
}
