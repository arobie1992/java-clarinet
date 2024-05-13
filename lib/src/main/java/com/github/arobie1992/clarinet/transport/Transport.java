package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.peer.Peer;

public interface Transport {
    void send(Peer peer, TransportOptions options, Object message);
    <T> T exchange(Peer peer, TransportOptions options, Object message, Class<T> responseType);
}
