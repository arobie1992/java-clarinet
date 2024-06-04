package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.adt.None;

public non-sealed interface SendHandler<I> extends Handler<I, Void> {
    @Override
    None<Void> handle(RemoteInformation remoteInformation, I message);
}
