package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final ConnectRequest connectRequest = new ConnectRequest(
            ConnectionId.random(),
            PeerUtils.senderId(),
            new ConnectionOptions(PeerUtils.senderId())
    );

    private ExchangeHandler<ConnectRequest, ConnectResponse> handler;
    private ConnectionStore connectionStore;
    private SimpleNode node;
    private ConnectHandlerProxy connectHandlerProxy;
    private PeerStore peerStore;
    private ConnectionImpl connection;

    @BeforeEach
    public void setUp() {
        node = mock(SimpleNode.class);
        when(node.id()).thenReturn(PeerUtils.receiverId());

        //noinspection unchecked
        handler = (ExchangeHandler<ConnectRequest, ConnectResponse>) mock(ExchangeHandler.class);
        connectionStore = mock(ConnectionStore.class);
        connection = new ConnectionImpl(
                connectRequest.connectionId(),
                connectRequest.sender(),
                PeerUtils.receiverId(),
                Connection.Status.AWAITING_WITNESS
        );
        connection.lock.writeLock().lock();
        when(connectionStore.accept(connectRequest.connectionId(), connection.sender(), node.id(), Connection.Status.AWAITING_WITNESS))
                .thenReturn(new Writeable(connection));
        connectHandlerProxy = new ConnectHandlerProxy(null, connectionStore, node);
        peerStore = mock(PeerStore.class);
        when(node.peerStore()).thenReturn(peerStore);
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.empty());
    }

    @Test
    void testNullHandler() {
        var expected = new Some<>(new ConnectResponse(false, null));
        var actual = connectHandlerProxy.handle(remoteInformation, connectRequest);
        assertEquals(expected, actual);
        verify(peerStore).save(remoteInformation.peer());
        verify(node, never()).selectWitness(any(), any(), any());
    }

    @Test
    void testNullConnectionStore() {
        assertThrows(NullPointerException.class, () -> new ConnectHandlerProxy(handler, null, node));
    }

    @Test
    void testNullNode() {
        assertThrows(NullPointerException.class, () -> new ConnectHandlerProxy(handler, connectionStore, null));
    }

    @Test
    void testUserHandlerRejects() {
        connectHandlerProxy = new ConnectHandlerProxy(handler, connectionStore, node);
        var expected = new Some<>(new ConnectResponse(true, "test reject"));
        when(handler.handle(remoteInformation, connectRequest)).thenReturn(expected);
        assertEquals(expected, connectHandlerProxy.handle(remoteInformation, connectRequest));
        verify(connectionStore, never()).accept(any(), any(), any(), any());
        verify(peerStore).save(remoteInformation.peer());
        verify(node, never()).selectWitness(any(), any(), any());
    }

    @Test
    void testDefaultHandlerInputType() {
        assertEquals(ConnectRequest.class, connectHandlerProxy.inputType());
    }

    @Test
    void testUpdatesStoredPeer() {
        var storedPeer = new Peer(remoteInformation.peer().id());
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.of(storedPeer));
        connectHandlerProxy.handle(remoteInformation, connectRequest);
        assertEquals(remoteInformation.peer().addresses(), storedPeer.addresses());
        verify(peerStore).save(storedPeer);
        verify(node, never()).selectWitness(any(), any(), any());
    }

    @Test
    void testUserHandlerReturnsNull() {
        // the mock returns null by default so we don't need to do any additional setup
        connectHandlerProxy = assertDoesNotThrow(() -> new ConnectHandlerProxy(handler, connectionStore, node));
        var ex = assertThrows(NullPointerException.class, () -> connectHandlerProxy.handle(remoteInformation, connectRequest));
        assertEquals("User handler returned a null ConnectResponse", ex.getMessage());
    }

    @Test
    void testReceiverSelectsWitness() {
        var request = new ConnectRequest(
                connectRequest.connectionId(),
                connectRequest.sender(),
                new ConnectionOptions(PeerUtils.receiverId())
        );
        connection = new ConnectionImpl(
                connectRequest.connectionId(),
                connectRequest.sender(),
                PeerUtils.receiverId(),
                Connection.Status.REQUESTING_WITNESS
        );
        connection.lock.writeLock().lock();
        when(connectionStore.accept(connectRequest.connectionId(), connection.sender(), node.id(), Connection.Status.REQUESTING_WITNESS))
                .thenReturn(new Writeable(connection));
        var expected = new Some<>(new ConnectResponse(false, null));
        var actual = connectHandlerProxy.handle(remoteInformation, request);
        assertEquals(expected, actual);
        verify(peerStore).save(remoteInformation.peer());
        verify(node).selectWitness(remoteInformation.peer(), connection, new TransportOptions());
    }

    @Test
    void testFailsToAcceptConnection() {
        when(connectionStore.accept(connectRequest.connectionId(), connection.sender(), node.id(), Connection.Status.AWAITING_WITNESS))
                .thenReturn(new Connection.Absent());
        var ex = assertThrows(IllegalStateException.class, () -> connectHandlerProxy.handle(remoteInformation, connectRequest));
        assertEquals("Failed to accept connection " + connectRequest.connectionId(), ex.getMessage());
    }

}