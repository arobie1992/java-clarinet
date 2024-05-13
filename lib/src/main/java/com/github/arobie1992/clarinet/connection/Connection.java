package com.github.arobie1992.clarinet.connection;

import com.github.arobie1992.clarinet.peer.Peer;

import java.util.Optional;

public interface Connection {
    ConnectionStatus status();
    Connection updateStatus(ConnectionStatus status);
    Peer sender();
    Optional<Peer> witness();
    Connection setWitness(Peer peer);
    Peer receiver();
}
