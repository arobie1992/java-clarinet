package com.github.arobie1992.clarinet.peer;

import java.net.URI;
import java.util.function.Function;

public interface Address {
    URI toURI();
    Function<URI, Address> parseFunction();
}
