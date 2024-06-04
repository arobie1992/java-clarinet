package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.Peer;

public record RemoteInformation(Peer peer, Address remoteAddress) {
}
