package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

public record WitnessRequest(ConnectionId connectionId, PeerId sender, PeerId receiver) {
}
