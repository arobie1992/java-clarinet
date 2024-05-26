package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.crypto.Key;
import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryKeyStore implements KeyStore {
    private final Map<PeerId, Collection<Key>> pubKeys = new ConcurrentHashMap<>();

    @Override
    public Collection<Key> findPublicKeys(PeerId peerId) {
        return List.copyOf(pubKeys.computeIfAbsent(peerId, k -> new ArrayList<>()));
    }
}
