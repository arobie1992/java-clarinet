package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.crypto.PrivateKey;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryKeyStoreTest {

    private record TestPrivateKey(int value) implements PrivateKey {
        @Override
        public byte[] sign(byte[] data) {
            return new byte[0];
        }
    }

    private record TestPublicKey(int value) implements PublicKey {
        @Override
        public boolean verify(byte[] data, byte[] signature) {
            return false;
        }
    }

    private InMemoryKeyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryKeyStore();
    }

    @Test
    void testPrivateKeyOperations() {
        var key = new TestPrivateKey(1);
        store.addPrivateKey(PeerUtils.senderId(), key);
        var keys = store.findPrivateKeys(PeerUtils.senderId());
        assertEquals(List.of(key), keys);
        var key2 = new TestPrivateKey(2);
        assertThrows(UnsupportedOperationException.class, () -> keys.add(key2));
        store.addPrivateKey(PeerUtils.senderId(), key2);
        assertEquals(List.of(key, key2), store.findPrivateKeys(PeerUtils.senderId()));
    }

    @Test
    void testPublicKeyOperations() {
        var key = new TestPublicKey(1);
        store.addPublicKey(PeerUtils.senderId(), key);
        var keys = store.findPublicKeys(PeerUtils.senderId());
        assertEquals(List.of(key), keys);
        var key2 = new TestPublicKey(2);
        assertThrows(UnsupportedOperationException.class, () -> keys.add(key2));
        store.addPublicKey(PeerUtils.senderId(), key2);
        assertEquals(List.of(key, key2), store.findPublicKeys(PeerUtils.senderId()));
    }

}