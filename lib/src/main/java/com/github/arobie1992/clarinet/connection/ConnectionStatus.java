package com.github.arobie1992.clarinet.connection;

public enum ConnectionStatus {
    REQUESTING_RECEIVER,
    REQUESTING_SENDER,
    AWAITING_WITNESS,
    REQUESTING_WITNESS,
    NOTIFYING_OF_WITNESS,
    OPEN,
    CLOSING,
    CLOSED
}
