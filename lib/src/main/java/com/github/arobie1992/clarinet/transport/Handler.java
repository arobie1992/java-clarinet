package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.adt.Option;

public sealed interface Handler<I, O> permits ExchangeHandler, SendHandler {
    Option<O> handle(RemoteInformation remoteInformation, I message);
    Class<I> inputType();
}
