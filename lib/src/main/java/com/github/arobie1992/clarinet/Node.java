package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionOptions;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.TransportOptions;

public interface Node {
    ConnectionId connect(PeerId peerId, Address address, ConnectionOptions connectionOptions, TransportOptions transportOptions);
}
