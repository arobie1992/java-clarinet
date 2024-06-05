package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PeersRequestHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final PeersRequest peersRequest = new PeersRequest(3);
    private final Peer sender = new Peer(PeerUtils.senderId(), Set.of(AddressUtils.defaultAddress()));
    private final Peer witness = new Peer(PeerUtils.witnessId(), Set.of(AddressUtils.defaultAddress()));
    private final Peer receiver = new Peer(PeerUtils.receiverId(), Set.of(AddressUtils.defaultAddress()));
    private final Peer nodePeer = new Peer(new StringPeerId("node"), Set.of(AddressUtils.defaultAddress()));

    private ExchangeHandler<PeersRequest, PeersResponse> handler;
    private Node node;
    private PeersRequestHandlerProxy proxy;
    private PeerStore peerStore;

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        handler = (ExchangeHandler<PeersRequest, PeersResponse>) mock(ExchangeHandler.class);
        node = mock(Node.class);
        proxy = new PeersRequestHandlerProxy(null, node);
        peerStore = mock(PeerStore.class);

        when(node.peerStore()).thenReturn(peerStore);
        when(node.id()).thenReturn(nodePeer.id());
        var transportMock = mock(Transport.class);
        when(node.transport()).thenReturn(transportMock);
        when(transportMock.addresses()).thenReturn(nodePeer.addresses());

        when(peerStore.find(sender.id())).thenReturn(Optional.of(sender));
        when(peerStore.find(witness.id())).thenReturn(Optional.of(witness));
        when(peerStore.find(receiver.id())).thenReturn(Optional.of(receiver));
        when(peerStore.find(nodePeer.id())).thenReturn(Optional.empty());
        when(peerStore.all()).thenReturn(Stream.of(sender.id(), witness.id(), receiver.id()));
    }

    @Test
    void testUserHandlerHonored() {
        proxy = new PeersRequestHandlerProxy(handler, node);
        var expected = new Some<>(new PeersResponse(List.of()));
        when(handler.handle(remoteInformation, peersRequest)).thenReturn(expected);
        assertEquals(expected, proxy.handle(remoteInformation, peersRequest));
        verify(node, never()).peerStore();
    }

    @Test
    void testUserHandlerReturnsNull() {
        proxy = new PeersRequestHandlerProxy(handler, node);
        when(handler.handle(remoteInformation, peersRequest)).thenReturn(null);
        var ex = assertThrows(NullPointerException.class, () -> proxy.handle(remoteInformation, peersRequest));
        assertEquals("userHandler returned null", ex.getMessage());
    }

    @Test
    void testNoAdditionalRequested() {
        var request = new PeersRequest(Set.of(witness.id(), receiver.id()));
        when(peerStore.find(PeerUtils.witnessId())).thenReturn(Optional.empty());

        var resp = proxy.handle(remoteInformation, request);
        var peersResponse = resp.value();
        verifyExpectedPeers(peersResponse, receiver);
    }

    @Test
    void testAdditionalRequested() {
        var request = new PeersRequest(Set.of(witness.id()), 2);
        var resp = proxy.handle(remoteInformation, request);
        var peersResponse = resp.value();
        verifyExpectedPeers(peersResponse, witness, receiver, sender);
    }

    @Test
    void testOnlyAdditionalRequested() {
        var request = new PeersRequest( 2);
        var resp = proxy.handle(remoteInformation, request);
        var peersResponse = resp.value();
        verifySomeExpectedPeers(peersResponse, 2, witness, receiver, sender);
    }

    @Test
    void testFewerCandidatesThanRequested() {
        var request = new PeersRequest( 5);
        var resp = proxy.handle(remoteInformation, request);
        var peersResponse = resp.value();
        verifyExpectedPeers(peersResponse, witness, receiver, sender);
    }

    @Test
    void testRequestForNode() {
        var request = new PeersRequest( Set.of(nodePeer.id()));
        var resp = proxy.handle(remoteInformation, request);
        var peersResponse = resp.value();
        verifyExpectedPeers(peersResponse, nodePeer);
    }

    @Test
    void testInputType() {
        assertEquals(PeersRequest.class, proxy.inputType());
    }

    private void verifySomeExpectedPeers(
            PeersResponse response,
            @SuppressWarnings("SameParameterValue") int numExpected,
            Peer... expectedCandidates
    ) {
        int numFound = 0;
        for(var peer : expectedCandidates) {
            for(var p2 : response.peers()) {
                if(p2.id().equals(peer.id())) {
                    numFound++;
                    assertEquals(
                            peer.addresses(),
                            p2.addresses(),
                            String.format("peer %s expected: %s, actual: %s", peer.id(), peer.addresses(), p2.addresses())
                    );
                    break;
                }
            }
        }
        assertEquals(numExpected, numFound);
    }

    private void verifyExpectedPeers(PeersResponse response, Peer... expected) {
        assertEquals(expected.length, response.peers().size());
        for(var peer : expected) {
            var found = true;
            for(var p2 : response.peers()) {
                if(p2.id().equals(peer.id())) {
                    assertEquals(peer.addresses(), p2.addresses());
                    break;
                }
            }
            assertTrue(found, "peer " + peer.id() + " not found.");
        }
    }
}