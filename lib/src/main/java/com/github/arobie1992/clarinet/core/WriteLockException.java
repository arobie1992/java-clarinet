package com.github.arobie1992.clarinet.core;

public final class WriteLockException extends RuntimeException {
    private final String operationName;

    public WriteLockException(String operationName) {
        super("Write lock must be held to perform operation " + operationName);
        this.operationName = operationName;
    }

    public String operationName() {
        return operationName;
    }
}
