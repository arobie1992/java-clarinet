package com.github.arobie1992.clarinet.transport;

import java.net.URISyntaxException;

public class UncheckedURISyntaxException extends RuntimeException {
    public UncheckedURISyntaxException(URISyntaxException cause) {
        super(cause);
    }
    public URISyntaxException getCause() {
        return (URISyntaxException) super.getCause();
    }
}
