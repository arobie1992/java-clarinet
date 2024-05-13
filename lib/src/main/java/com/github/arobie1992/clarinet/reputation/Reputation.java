package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

public interface Reputation {
    PeerId peerID();
    double value();
    Reputation strongPenalize();
    Reputation weakPenalize();
    Reputation reward();
}
