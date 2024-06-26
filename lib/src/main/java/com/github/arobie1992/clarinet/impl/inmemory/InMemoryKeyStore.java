package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.crypto.*;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class InMemoryKeyStore implements KeyStore {
    private final Map<PeerId, Collection<PrivateKey>> privKeys = new ConcurrentHashMap<>();
    private final Map<PeerId, Collection<PublicKey>> pubKeys = new ConcurrentHashMap<>();
    private final List<KeyProvider> keyProviders = new ArrayList<>();

    @Override
    public void addKeyPair(PeerId peerId, KeyPair keyPair) {
        addPrivateKey(peerId, keyPair.privateKey());
        addPublicKey(peerId, keyPair.publicKey());
    }

    @Override
    public void addPrivateKey(PeerId peerId, PrivateKey privateKey) {
        privKeys.compute(peerId, (k, existing) -> {
            if (existing == null) {
                existing = new ArrayList<>();
            }
            existing.add(privateKey);
            return existing;
        });
    }

    @Override
    public Collection<PrivateKey> findPrivateKeys(PeerId peerId) {
        return List.copyOf(privKeys.computeIfAbsent(peerId, k -> new ArrayList<>()));
    }

    @Override
    public void addPublicKey(PeerId peerId, PublicKey publicKey) {
        pubKeys.compute(peerId, (k, existing) -> {
            if (existing == null) {
                existing = new ArrayList<>();
            }
            existing.add(publicKey);
            return existing;
        });
    }

    @Override
    public Collection<PublicKey> findPublicKeys(PeerId peerId) {
        return List.copyOf(pubKeys.computeIfAbsent(peerId, k -> new ArrayList<>()));
    }

    @Override
    public void addProvider(KeyProvider keyProvider) {
        keyProviders.add(keyProvider);
    }

    @Override
    public Stream<KeyProvider> providers() {
        return keyProviders.stream();
    }
}
