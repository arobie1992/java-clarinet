package com.github.arobie1992.clarinet.testutils;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Reputation;

public class TestReputation implements Reputation {
    private final PeerId peerId;
    public int rewards = 0;
    public int weakPenalties = 0;
    public int strongPenalties = 0;
    public int value = 1;

    public TestReputation(PeerId peerId) {
        this.peerId = peerId;
    }

    @Override
    public PeerId peerId() {
        return peerId;
    }

    @Override
    public double value() {
        return value;
    }

    @Override
    public void strongPenalize() {
        strongPenalties++;
    }

    @Override
    public void weakPenalize() {
        weakPenalties++;
    }

    @Override
    public void reward() {
        rewards++;
    }

    @Override
    public Reputation copy() {
        var copy = new TestReputation(peerId);
        copy.rewards = rewards;
        copy.weakPenalties = weakPenalties;
        copy.strongPenalties = strongPenalties;
        copy.value = value;
        return copy;
    }
}
