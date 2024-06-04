package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Collection;

public record Message(String endpoint, PeerId sender, Collection<? extends Address> contactAt, Object contents) {
}
