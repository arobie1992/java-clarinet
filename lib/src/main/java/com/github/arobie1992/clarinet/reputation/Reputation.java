package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

public interface Reputation {
    PeerId peerId();

    /**
     * A {@code double} representation of the reputation score.
     * <p>
     * This must always be between 0.0 and 1.0.
     * @return the {@code double} representation
     */
    double value();
    void strongPenalize();
    void weakPenalize();

    void reward();

    Reputation copy();
}
