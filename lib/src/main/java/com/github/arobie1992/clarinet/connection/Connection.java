package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Optional;

public sealed interface Connection permits ConnectionImpl {
    ConnectionId id();
    PeerId sender();
    Optional<PeerId> witness();
    PeerId receiver();
    Connection.Status status();

    enum Status {
        REQUESTING_RECEIVER,
        REQUESTING_SENDER,
        AWAITING_WITNESS,
        REQUESTING_WITNESS,
        NOTIFYING_OF_WITNESS,
        OPEN,
        CLOSING,
        CLOSED
    }
}
