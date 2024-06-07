package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Collection;
import java.util.stream.Stream;

public interface KeyStore {
    void addKeyPair(PeerId peerId, KeyPair keyPair);
    void addPrivateKey(PeerId peerId, PrivateKey privateKey);
    Collection<PrivateKey> findPrivateKeys(PeerId peerId);
    void addPublicKey(PeerId peerId, PublicKey publicKey);
    Collection<PublicKey> findPublicKeys(PeerId peerId);
    void addProvider(KeyProvider keyProvider);
    Stream<KeyProvider> providers();
}
