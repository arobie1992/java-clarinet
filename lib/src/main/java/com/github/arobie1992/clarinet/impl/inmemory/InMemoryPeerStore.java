package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class InMemoryPeerStore implements PeerStore {

    private final Map<PeerId, Peer> peers = new HashMap<>();

    @Override
    public void save(Peer peer) {
        peers.put(peer.id(), peer);
    }

    @Override
    public Stream<Peer> all() {
        return peers.values().stream().map(p -> {
            var copy = new Peer(p.id());
            copy.addresses().addAll(p.addresses());
            return copy;
        });
    }

}
