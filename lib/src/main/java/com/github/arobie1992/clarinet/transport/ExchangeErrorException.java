package com.github.arobie1992.clarinet.transport;

public class ExchangeErrorException extends RuntimeException {
    private final String error;

    public ExchangeErrorException(String error) {
        super("Received the following error during the exchange: " + error);
        this.error = error;
    }
    public String error() {
        return error;
    }
}
