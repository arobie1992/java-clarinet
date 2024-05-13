package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.PeerId;

public record ConnectRequest(ConnectionId connectionId, PeerId sender, PeerId receiver, ConnectionOptions options) {}
