package com.github.arobie1992.clarinet.transport;

import java.util.List;

public class ExchangeErrorsException extends RuntimeException {
    private final List<String> errors;

    public ExchangeErrorsException(List<String> errors) {
        super("Received the following errors during the exchange: " + errors);
        this.errors = List.copyOf(errors);
    }
    public List<String> errors() {
        return errors;
    }
}
