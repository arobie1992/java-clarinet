package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.peer.Address;

public non-sealed interface ExchangeHandler<I, O> extends Handler<I, O> {
    @Override
    Some<O> handle(Address remoteAddress, I message);
}
