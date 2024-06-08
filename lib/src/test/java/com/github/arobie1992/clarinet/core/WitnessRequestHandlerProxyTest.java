package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WitnessRequestHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final WitnessRequest witnessRequest = new WitnessRequest(ConnectionId.random(), PeerUtils.senderId(), PeerUtils.receiverId());

    private ExchangeHandler<WitnessRequest, WitnessResponse> handler;
    private ConnectionStore connectionStore;
    private Node node;
    private WitnessRequestHandlerProxy handlerProxy;
    private PeerStore peerStore;
    private ConnectionImpl connection;

    @BeforeEach
    public void setUp() {
        //noinspection unchecked
        handler = (ExchangeHandler<WitnessRequest, WitnessResponse>) mock(ExchangeHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(Node.class);
        handlerProxy = new WitnessRequestHandlerProxy(null, connectionStore, node);
        peerStore = mock(PeerStore.class);
        when(node.peerStore()).thenReturn(peerStore);
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.empty());
        connection = new ConnectionImpl(witnessRequest.connectionId(), witnessRequest.sender(), witnessRequest.receiver(), Connection.Status.OPEN);
        connection.lock.writeLock().lock();
        when(connectionStore.accept(witnessRequest.connectionId(), connection.sender(), connection.receiver(), Connection.Status.OPEN))
                .thenReturn(new Writeable(connection));
    }


    @Test
    void testNullHandler() {
        var expected = new Some<>(new WitnessResponse(false, null));
        when(node.id()).thenReturn(PeerUtils.witnessId());
        var actual = handlerProxy.handle(remoteInformation, witnessRequest);
        assertEquals(expected, actual);
        verify(connectionStore).accept(
                witnessRequest.connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.OPEN
        );
        assertEquals(PeerUtils.witnessId(), connection.witness().orElseThrow());
        verify(peerStore).save(remoteInformation.peer());
    }

    @Test
    void testNullConnectionStore() {
        assertThrows(NullPointerException.class, () -> new WitnessRequestHandlerProxy(handler, null, node));
    }

    @Test
    void testNullNode() {
        assertThrows(NullPointerException.class, () -> new WitnessRequestHandlerProxy(handler, connectionStore, null));
    }

    @Test
    void testUserHandlerRejects() {
        handlerProxy = new WitnessRequestHandlerProxy(handler, connectionStore, node);
        var expected = new Some<>(new WitnessResponse(true, "test reject"));
        when(handler.handle(remoteInformation, witnessRequest)).thenReturn(expected);
        assertEquals(expected, handlerProxy.handle(remoteInformation, witnessRequest));
        verify(connectionStore, never()).accept(any(), any(), any(), any());
        verify(peerStore).save(remoteInformation.peer());
    }

    @Test
    void handleFailsToFindConnection() {
        when(node.id()).thenReturn(PeerUtils.witnessId());
        when(connectionStore.accept(witnessRequest.connectionId(), connection.sender(), connection.receiver(), Connection.Status.OPEN))
                .thenReturn(new Connection.Absent());
        var ex = assertThrows(IllegalStateException.class, () -> handlerProxy.handle(remoteInformation, witnessRequest));
        assertEquals("Failed to accept connection", ex.getMessage());
        verify(connectionStore).accept(
                witnessRequest.connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.OPEN
        );
        verify(peerStore).save(remoteInformation.peer());
    }

    @Test
    void testDefaultHandlerInputType() {
        assertEquals(WitnessRequest.class, handlerProxy.inputType());
    }

    @Test
    void testStoredPeerUpdated() {
        var storedPeer = new Peer(remoteInformation.peer().id());
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.of(storedPeer));
        handlerProxy.handle(remoteInformation, witnessRequest);
        assertEquals(remoteInformation.peer().addresses(), storedPeer.addresses());
        verify(peerStore).save(storedPeer);
    }

    @Test
    void testUserHandlerReturnsNull() {
        handlerProxy = assertDoesNotThrow(() -> new WitnessRequestHandlerProxy(handler, connectionStore, node));
        var ex = assertThrows(NullPointerException.class, () -> handlerProxy.handle(remoteInformation, witnessRequest));
        assertEquals("User handler returned a null WitnessResponse", ex.getMessage());
    }

}