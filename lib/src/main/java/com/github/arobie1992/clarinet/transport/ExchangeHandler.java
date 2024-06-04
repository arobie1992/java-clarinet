package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.adt.Some;

public non-sealed interface ExchangeHandler<I, O> extends Handler<I, O> {
    @Override
    Some<O> handle(RemoteInformation remoteInformation, I message);
}
