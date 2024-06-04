package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Peer;

import java.util.List;

public record PeersResponse(List<Peer> peers) {
}
