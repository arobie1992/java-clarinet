package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WitnessHandlerProxyTest {

    private final WitnessRequest witnessRequest = new WitnessRequest(ConnectionId.random(), PeerUtils.senderId(), PeerUtils.receiverId());

    private Handler<WitnessRequest, WitnessResponse> handler;
    private ConnectionStore connectionStore;
    private Node node;
    private WitnessHandlerProxy handlerProxy;

    @BeforeEach
    public void setUp() {
        //noinspection unchecked
        handler = (Handler<WitnessRequest, WitnessResponse>) mock(Handler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(Node.class);
        handlerProxy = new WitnessHandlerProxy(handler, connectionStore, node);
    }


    @Test
    void testNullHandler() {
        handlerProxy = assertDoesNotThrow(() -> new WitnessHandlerProxy(null, connectionStore, node));
        var expected = new WitnessResponse(false, null);
        when(node.id()).thenReturn(PeerUtils.witnessId());
        var conn = new ConnectionImpl(witnessRequest.connectionId(), witnessRequest.sender(), witnessRequest.receiver(), Connection.Status.OPEN);
        conn.lock.writeLock().lock();
        when(connectionStore.findForWrite(witnessRequest.connectionId())).thenReturn(new Writeable(conn));
        var actual = handlerProxy.handle(AddressUtils.defaultAddress(), witnessRequest);
        assertEquals(expected, actual);
        verify(connectionStore).accept(
                witnessRequest.connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.OPEN
        );
        assertEquals(PeerUtils.witnessId(), conn.witness().orElseThrow());
    }

    @Test
    void testNullConnectionStore() {
        assertThrows(NullPointerException.class, () -> new WitnessHandlerProxy(handler, null, node));
    }

    @Test
    void testNullNode() {
        assertThrows(NullPointerException.class, () -> new WitnessHandlerProxy(handler, connectionStore, null));
    }

    @Test
    void testUserHandlerRejects() {
        var expected = new WitnessResponse(true, "test reject");
        when(handler.handle(AddressUtils.defaultAddress(), witnessRequest)).thenReturn(expected);
        assertEquals(expected, handlerProxy.handle(AddressUtils.defaultAddress(), witnessRequest));
        verify(connectionStore, never()).accept(any(), any(), any(), any());
    }

    @Test
    void handleFailsToFindConnection() {
        handlerProxy = assertDoesNotThrow(() -> new WitnessHandlerProxy(null, connectionStore, node));
        when(node.id()).thenReturn(PeerUtils.witnessId());
        when(connectionStore.findForWrite(witnessRequest.connectionId())).thenReturn(new Connection.Absent());
        var ex = assertThrows(IllegalStateException.class, () -> handlerProxy.handle(AddressUtils.defaultAddress(), witnessRequest));
        assertEquals("Failed to accept connection", ex.getMessage());
        verify(connectionStore).accept(
                witnessRequest.connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.OPEN
        );
    }

    @Test
    void testDefaultHandlerInputType() {
        handlerProxy = assertDoesNotThrow(() -> new WitnessHandlerProxy(null, connectionStore, node));
        assertEquals(WitnessRequest.class, handlerProxy.inputType());
    }

}