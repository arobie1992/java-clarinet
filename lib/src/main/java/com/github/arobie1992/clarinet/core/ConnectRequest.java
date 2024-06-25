package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Objects;

public record ConnectRequest(ConnectionId connectionId, PeerId sender, ConnectionOptions options) {
    public ConnectRequest {
        Objects.requireNonNull(connectionId);
        Objects.requireNonNull(sender);
        Objects.requireNonNull(options);
    }
}
