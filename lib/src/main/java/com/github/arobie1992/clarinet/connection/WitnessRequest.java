package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.PeerId;

public record WitnessRequest(ConnectionId connectionId, PeerId sender, PeerId receiver, ConnectionOptions option) {}
