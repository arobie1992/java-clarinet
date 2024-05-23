package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectHandlerProxyTest {

    private final ConnectRequest connectRequest = new ConnectRequest(ConnectionId.random(), PeerUtils.senderId());

    private Handler<ConnectRequest, ConnectResponse> handler;
    private ConnectionStore connectionStore;
    private Node node;
    private ConnectHandlerProxy connectHandlerProxy;

    @BeforeEach
    public void setUp() {
        //noinspection unchecked
        handler = (Handler<ConnectRequest, ConnectResponse>) mock(Handler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(Node.class);
        connectHandlerProxy = new ConnectHandlerProxy(handler, connectionStore, node);
    }

    @Test
    void testNullHandler() {
        connectHandlerProxy = assertDoesNotThrow(() -> new ConnectHandlerProxy(null, connectionStore, node));
        var expected = new ConnectResponse(false, null);
        when(node.id()).thenReturn(PeerUtils.receiverId());
        var actual = connectHandlerProxy.handle(AddressUtils.defaultAddress(), connectRequest);
        assertEquals(expected, actual);
        verify(connectionStore).accept(
                connectRequest.connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.AWAITING_WITNESS
        );
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
        var expected = new ConnectResponse(true, "test reject");
        when(handler.handle(AddressUtils.defaultAddress(), connectRequest)).thenReturn(expected);
        assertEquals(expected, connectHandlerProxy.handle(AddressUtils.defaultAddress(), connectRequest));
        verify(connectionStore, never()).accept(any(), any(), any(), any());
    }

    @Test
    void testDefaultHandlerInputType() {
        connectHandlerProxy = assertDoesNotThrow(() -> new ConnectHandlerProxy(null, connectionStore, node));
        assertEquals(ConnectRequest.class, connectHandlerProxy.inputType());
    }

}