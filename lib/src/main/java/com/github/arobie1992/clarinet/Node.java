package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.connection.Connection;
import com.github.arobie1992.clarinet.connection.ConnectionId;
import com.github.arobie1992.clarinet.connection.ConnectionOptions;
import com.github.arobie1992.clarinet.data.MessageID;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeersRequest;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.List;

public interface Node {
    List<Connection> connections();
    List<Peer> peers();
    Peer self();
    void updatePeer(Peer peer);
    ConnectionId connect(Peer peer, ConnectionOptions connectionOptions, TransportOptions transportOptions);
    void closeConnection(ConnectionId connectionId, TransportOptions transportOptions);
    void requestPeers(Peer peer, PeersRequest request, TransportOptions transportOptions);
    MessageID send(ConnectionId connectionID, TransportOptions transportOptions, byte[] data);
}
