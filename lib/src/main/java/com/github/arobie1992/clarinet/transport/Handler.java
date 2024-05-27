package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.adt.Option;
import com.github.arobie1992.clarinet.peer.Address;

public sealed interface Handler<I, O> permits ExchangeHandler, SendHandler {
    Option<O> handle(Address remoteAddress, I message);
    Class<I> inputType();
}
