package com.github.arobie1992.clarinet.transport;

public class MismatchedResponseTypeException extends RuntimeException {
    private final String response;
    private final Class<?> responseType;

    public MismatchedResponseTypeException(String response, Class<?> responseType, Throwable cause) {
        super("Failed to parse response as type " + responseType, cause);
        this.response = response;
        this.responseType = responseType;
    }

    public String response() {
        return response;
    }

    public Class<?> responseType() {
        return responseType;
    }
}
