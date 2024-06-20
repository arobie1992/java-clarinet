package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.crypto.KeyPair;
import com.github.arobie1992.clarinet.crypto.PrivateKey;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.PublicKeyProvider;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class InMemoryKeyStoreTest {

    private record TestPrivateKey(int value) implements PrivateKey {
        @Override
        public Bytes sign(Bytes data) {
            return Bytes.of(new byte[0]);
        }
        @Override
        public String algorithm() {
            return "";
        }
        @Override
        public Bytes bytes() {
            return Bytes.of(new byte[0]);
        }
    }

    private record TestPublicKey(int value) implements PublicKey {
        @Override
        public boolean verify(Bytes data, Bytes signature) {
            return false;
        }
        @Override
        public boolean verifyHash(Bytes hash, Bytes signature) {
            return false;
        }
        @Override
        public String algorithm() {
            return "";
        }
        @Override
        public Bytes bytes() {
            return Bytes.of(new byte[0]);
        }
    }

    private InMemoryKeyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryKeyStore();
    }

    @Test
    void testAddKeyPair() {
        var pair = new KeyPair(new TestPublicKey(1), new TestPrivateKey(2));
        store.addKeyPair(PeerUtils.senderId(), pair);
        assertEquals(List.of(pair.publicKey()), store.findPublicKeys(PeerUtils.senderId()));
        assertEquals(List.of(pair.privateKey()), store.findPrivateKeys(PeerUtils.senderId()));
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

    @Test
    void testAddProvider() {
        var provider = mock(PublicKeyProvider.class);
        store.addProvider(provider);
        assertEquals(List.of(provider), store.providers().toList());
    }

}