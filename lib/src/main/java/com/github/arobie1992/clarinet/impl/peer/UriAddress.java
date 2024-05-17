package com.github.arobie1992.clarinet.impl.peer;

import com.github.arobie1992.clarinet.peer.Address;

import java.net.URI;
import java.util.function.Function;

public record UriAddress(URI uri) implements Address {
    @Override
    public URI asURI() {
        return uri;
    }

    // TODO Probably going to come back to these parse functions to see if this setup is actually useful
    @Override
    public Function<URI, Address> parseFunction() {
        return UriAddress::new;
    }
}
