package com.github.arobie1992.clarinet.impl.netty;

public class NoSuchEndpointException extends RuntimeException {
    private final String endpoint;

    public NoSuchEndpointException(String endpoint) {
        super("No such endpoint: " + endpoint);
        this.endpoint = endpoint;
    }

    public String endpoint() {
        return endpoint;
    }
}
