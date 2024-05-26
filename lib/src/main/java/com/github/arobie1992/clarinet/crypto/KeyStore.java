package com.github.arobie1992.clarinet.crypto;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Collection;

public interface KeyStore {
    Collection<Key> findPublicKeys(PeerId peerId);
}
