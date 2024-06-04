package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WitnessNotificationHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final WitnessNotification witnessNotification = new WitnessNotification(ConnectionId.random(), PeerUtils.witnessId());

    private SendHandler<WitnessNotification> handler;
    private ConnectionStore connectionStore;
    private Node node;
    private WitnessNotificationHandlerProxy handlerProxy;
    private ConnectionImpl connection;
    private TestConnection expected;
    private PeerStore peerStore;

    @BeforeEach
    public void setUp() {
        //noinspection unchecked
        handler = (SendHandler<WitnessNotification>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(Node.class);
        handlerProxy = new WitnessNotificationHandlerProxy(null, connectionStore, node);
        connection = new ConnectionImpl(
                witnessNotification.connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.AWAITING_WITNESS
        );
        connection.lock.writeLock().lock();
        when(connectionStore.findForWrite(witnessNotification.connectionId())).thenReturn(new Writeable(connection));
        expected = new TestConnection(
                connection.id(),
                connection.sender(),
                Optional.of(witnessNotification.witness()),
                connection.receiver(),
                Connection.Status.OPEN
        );

        peerStore = mock(PeerStore.class);
        when(node.peerStore()).thenReturn(peerStore);
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.empty());
    }

    @Test
    void testUserHandler() {
        handlerProxy = new WitnessNotificationHandlerProxy(handler, connectionStore, node);
        handlerProxy.handle(remoteInformation, witnessNotification);
        verify(handler).handle(remoteInformation, witnessNotification);
        expected.assertMatches(connection);
    }

    @Test
    void testNullUserHandler() {
        handlerProxy.handle(remoteInformation, witnessNotification);
        expected.assertMatches(connection);
    }

    @Test
    void testNoConnection() {
        when(connectionStore.findForWrite(witnessNotification.connectionId())).thenReturn(new Connection.Absent());
        var ex = assertThrows(NoSuchConnectionException.class, () -> handlerProxy.handle(remoteInformation, witnessNotification));
        assertEquals(witnessNotification.connectionId(), ex.connectionId());
    }

    @Test
    void testDefaultHandlerInputType() {
        assertEquals(WitnessNotification.class, handlerProxy.inputType());
    }

    @Test
    void testConnectionNotAwaitingWitness() {
        connection.setStatus(Connection.Status.REQUESTING_RECEIVER);
        var ex = assertThrows(UnsupportedOperationException.class, () -> handlerProxy.handle(remoteInformation, witnessNotification));
        assertEquals("Connection " + witnessNotification.connectionId() + " is not awaiting witness.", ex.getMessage());
    }

    @Test
    void testStoredPeerUpdated() {
        var storedPeer = new Peer(remoteInformation.peer().id());
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.of(storedPeer));
        handlerProxy.handle(remoteInformation, witnessNotification);
        assertEquals(remoteInformation.peer().addresses(), storedPeer.addresses());
        verify(peerStore).save(storedPeer);
    }

}