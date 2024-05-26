package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.crypto.Key;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryKeyStoreTest {

    private record TestKey() implements Key {}

    private InMemoryKeyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryKeyStore();
    }

    @Test
    void testFindPublicKeysDoesNotAllowMutation() {
        var pubKeys = store.findPublicKeys(PeerUtils.senderId());
        assertThrows(UnsupportedOperationException.class, () -> pubKeys.add(new TestKey()));
    }

}