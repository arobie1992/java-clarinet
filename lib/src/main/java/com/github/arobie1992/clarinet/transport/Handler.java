package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.core.Response;

import java.util.Optional;

public interface Handler<T> {
    Optional<Response> handle(T message);
    Class<T> inputType();
}
