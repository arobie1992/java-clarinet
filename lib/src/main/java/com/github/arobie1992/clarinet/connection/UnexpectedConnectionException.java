package com.github.arobie1992.clarinet.connection;

public class UnexpectedConnectionException extends RuntimeException {
    private final ConnectionId expected;
    private final ConnectionId actual;

    public UnexpectedConnectionException(ConnectionId expected, ConnectionId actual) {
        super(String.format("Unexpected connection: expected: %s, actual: %s", expected, actual));
        this.expected = expected;
        this.actual = actual;
    }

    public ConnectionId expected() {
        return expected;
    }

    public ConnectionId actual() {
        return actual;
    }
}
