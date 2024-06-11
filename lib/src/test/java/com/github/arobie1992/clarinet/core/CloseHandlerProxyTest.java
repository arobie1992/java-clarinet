package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloseHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final CloseRequest closeRequest = new CloseRequest(ConnectionId.random());

    private SendHandler<CloseRequest> handler;
    private ConnectionStore connectionStore;
    private CloseHandlerProxy proxy;
    private ConnectionImpl connection;

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        handler = (SendHandler<CloseRequest>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        proxy = new CloseHandlerProxy(null, connectionStore);
        connection = new ConnectionImpl(closeRequest.connectionId(), PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        connection.lock.writeLock().lock();

        when(connectionStore.findForWrite(closeRequest.connectionId())).thenReturn(new Writeable(connection));
    }

    @Test
    void testNullConnectionStore() {
        assertThrows(NullPointerException.class, () -> new CloseHandlerProxy(handler, null));
    }

    @Test
    void testDefaultHandler() {
        proxy.handle(remoteInformation, closeRequest);
        assertEquals(Connection.Status.CLOSED, connection.status());
    }

    @Test
    void testNoConnection() {
        when(connectionStore.findForWrite(closeRequest.connectionId())).thenReturn(new Connection.Absent());
        var ex = assertThrows(NoSuchConnectionException.class, () -> proxy.handle(remoteInformation, closeRequest));
        assertEquals(closeRequest.connectionId(), ex.connectionId());
    }

    @Test
    void testUserHandlerCalled() {
        proxy = new CloseHandlerProxy(handler, connectionStore);
        proxy.handle(remoteInformation, closeRequest);
        assertEquals(Connection.Status.CLOSED, connection.status());
        verify(handler).handle(remoteInformation, closeRequest);
    }

    @Test
    void testDefaultHandlerInputType() {
        assertEquals(CloseRequest.class, proxy.inputType());
    }

    @Test
    void testUserHandlerInputType() {
        proxy = new CloseHandlerProxy(handler, connectionStore);
        when(handler.inputType()).thenReturn(CloseRequest.class);
        assertEquals(CloseRequest.class, proxy.inputType());
    }

}