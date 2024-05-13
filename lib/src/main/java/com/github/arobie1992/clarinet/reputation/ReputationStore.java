package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ReputationStore {
    void update(PeerId peerId, Function<? extends Reputation, ? extends Reputation> updateFunction);
    void read(PeerId peerId, Consumer<? extends Reputation> readFunction);
}
