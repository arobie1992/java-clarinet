package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.peer.Address;

public non-sealed interface SendHandler<I> extends Handler<I, Void> {
    @Override
    None<Void> handle(Address remoteAddress, I message);
}
