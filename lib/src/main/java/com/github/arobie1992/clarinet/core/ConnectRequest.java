package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

public record ConnectRequest(ConnectionId connectionId, PeerId sender) {
}
