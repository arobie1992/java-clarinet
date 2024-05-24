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
        var peerOpt = store.find(peers.getFirst());
        assertTrue(peerOpt.isPresent());
        var peer = peerOpt.get();
        assertEquals(PeerUtils.senderId(), peer.id());
        assertTrue(peer.addresses().isEmpty());
    }

    @Test
    void testSavePersistsUpdates() throws URISyntaxException {
        store.save(new Peer(PeerUtils.senderId()));
        var peer = store.find(PeerUtils.senderId()).orElseThrow();
        var address = new UriAddress(new URI("tcp://localhost"));
        peer.addresses().add(address);
        store.save(peer);
        var peers = store.all().toList();
        assertEquals(1, peers.size());
        assertEquals(Set.of(address), peer.addresses());
    }

    @Test
    void testFindNotPresent() {
        var peerOpt = store.find(PeerUtils.senderId());
        assertTrue(peerOpt.isEmpty());
    }

    @Test
    void testFindDoesNotPersistMutation() throws URISyntaxException {
        store.save(new Peer(PeerUtils.senderId()));
        var peerOpt = store.find(PeerUtils.senderId());
        assertTrue(peerOpt.isPresent());
        var peer = peerOpt.get();
        assertEquals(PeerUtils.senderId(), peer.id());
        peer.addresses().add(new UriAddress(new URI("tcp://localhost")));
        var unupdatedPeer = store.find(PeerUtils.senderId()).orElseThrow();
        assertTrue(unupdatedPeer.addresses().isEmpty());
    }

}