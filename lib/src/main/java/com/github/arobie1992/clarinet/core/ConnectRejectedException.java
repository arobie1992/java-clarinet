package com.github.arobie1992.clarinet.core;

public class ConnectRejectedException extends RuntimeException {
    private final String reason;

    public ConnectRejectedException(String reason) {
        super("Connect request rejected with given reason: " + reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
