package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryPeerStoreTest {

    private InMemoryPeerStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPeerStore();
    }

    @Test
    void testSave() {
        store.save(new Peer(PeerUtils.senderId()));
        var peers = store.all().toList();
        assertEquals(1, peers.size());
        var peer = peers.getFirst();
        assertEquals(PeerUtils.senderId(), peer.id());
        assertTrue(peer.addresses().isEmpty());
    }

    @Test
    void testSavePersistsUpdates() throws URISyntaxException {
        store.save(new Peer(PeerUtils.senderId()));
        var peer = store.all().findFirst().orElseThrow();
        var address = new UriAddress(new URI("tcp://localhost"));
        peer.addresses().add(address);
        store.save(peer);
        var peers = store.all().toList();
        assertEquals(1, peers.size());
        assertEquals(Set.of(address), peer.addresses());
    }

    @Test
    void testAllDoesNotAllowMutation() throws URISyntaxException {
        store.save(new Peer(PeerUtils.senderId()));
        var peer = store.all().findFirst().orElseThrow();
        peer.addresses().add(new UriAddress(new URI("tcp://localhost")));
        var unupdatedPeer = store.all().findFirst().orElseThrow();
        assertTrue(unupdatedPeer.addresses().isEmpty());
    }

}