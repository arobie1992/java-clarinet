package com.github.arobie1992.clarinet.impl.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Reputation;

public class ProportionalReputation implements Reputation {
    private final PeerId peerId;
    private double good;
    private double total;

    public ProportionalReputation(PeerId peerId) {
        this(peerId, 0, 0);
    }

    private ProportionalReputation(PeerId peerId, double good, double total) {
        this.peerId = peerId;
        this.good = good;
        this.total = total;
    }

    @Override
    public PeerId peerId() {
        return peerId;
    }

    @Override
    public double value() {
        return total == 0 ? 1 : good/total;
    }

    @Override
    public void strongPenalize() {
        total += 3;
    }

    @Override
    public void weakPenalize() {
        total += 1;
    }

    @Override
    public void reward() {
        good += 1;
        total += 1;
    }

    @Override
    public Reputation copy() {
        return new ProportionalReputation(peerId, good, total);
    }
}
