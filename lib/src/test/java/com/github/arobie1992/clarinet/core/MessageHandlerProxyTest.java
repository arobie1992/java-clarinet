package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.message.MessageStore;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageHandlerProxyTest {

    private final Address address = AddressUtils.defaultAddress();
    private final DataMessage message = new DataMessage(new MessageId(ConnectionId.random(), 0), new byte[]{77, 50, 126});
    private final byte[] witnessSignature = {45};

    private SendHandler<DataMessage> userHandler;
    private ConnectionStore connectionStore;
    private SimpleNode node;
    private MessageHandlerProxy proxy;
    private ConnectionImpl connection;
    private MessageStore messageStore;

    MessageHandlerProxyTest() {
        message.setSenderSignature(new byte[]{9, 9, 9});
    }

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        userHandler = (SendHandler<DataMessage>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(SimpleNode.class);
        proxy = new MessageHandlerProxy(null, connectionStore, node);

        connection = new ConnectionImpl(message.messageId().connectionId(), PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        // this gets thrown away after each test so it should be fine
        connection.lock.writeLock().lock();
        connection.setWitness(PeerUtils.witnessId());
        when(connectionStore.findForWrite(message.messageId().connectionId())).thenReturn(new Writeable(connection));

        when(node.id()).thenReturn(PeerUtils.witnessId());
        // don't know why the mock isn't working when I pass the witness parts (probably a dumb mistake), so do this for now.
        // does mean we need to be careful about calling genSignature
        when(node.genSignature(any())).thenReturn(witnessSignature);

        messageStore = mock(MessageStore.class);
        when(node.messageStore()).thenReturn(messageStore);
    }

    @Test
    void testNullConnectionStore() {
        assertThrows(NullPointerException.class, () -> new MessageHandlerProxy(userHandler, null, node));
    }

    @Test
    void testNullNode() {
        assertThrows(NullPointerException.class, () -> new MessageHandlerProxy(userHandler, connectionStore, null));
    }

    @Test
    void testConnectionAbsent() {
        when(connectionStore.findForWrite(message.messageId().connectionId())).thenReturn(new Connection.Absent());
        var ex = assertThrows(NoSuchConnectionException.class, () -> proxy.handle(address, message));
        assertEquals(message.messageId().connectionId(), ex.connectionId());
    }

    @Test
    void testConnectionNotOpen() {
        connection.setStatus(Connection.Status.REQUESTING_RECEIVER);
        var ex = assertThrows(ConnectionStatusException.class, () -> proxy.handle(address, message));
        assertEquals(message.messageId().connectionId(), ex.connectionId());
        assertEquals("send", ex.operation());
        assertEquals(Connection.Status.REQUESTING_RECEIVER, ex.status());
        assertEquals(List.of(Connection.Status.OPEN), ex.permittedStatuses());
    }

    @Test
    void testConnectionNoWitness() {
        connection = new ConnectionImpl(message.messageId().connectionId(), PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        connection.lock.writeLock().lock();
        when(connectionStore.findForWrite(message.messageId().connectionId())).thenReturn(new Writeable(connection));
        assertThrows(NoSuchElementException.class, () -> proxy.handle(address, message));
    }

    @Test
    void testUserHandlerCalledForWitness() {
        proxy = new MessageHandlerProxy(userHandler, connectionStore, node);
        proxy.handle(address, message);
        verify(messageStore).add(message);
        verify(userHandler).handle(address, message);
        verify(node).sendInternal(connection.receiver(), message, TransportUtils.defaultOptions());
        assertArrayEquals(witnessSignature, message.witnessSignature().orElseThrow());
    }

    @Test
    void testUserHandlerCalledForReceiver() {
        proxy = new MessageHandlerProxy(userHandler, connectionStore, node);
        when(node.id()).thenReturn(PeerUtils.receiverId());
        proxy.handle(address, message);
        verify(messageStore).add(message);
        verify(userHandler).handle(address, message);
        verify(node, never()).sendInternal(connection.receiver(), message, TransportUtils.defaultOptions());
        assertTrue(message.witnessSignature().isEmpty());
    }

    @Test
    void testNodeNotWitnessOrReceiver() {
        when(node.id()).thenReturn(PeerUtils.senderId());
        var ex = assertThrows(IllegalArgumentException.class, () -> proxy.handle(address, message));
        assertEquals("Connection is not through or to " + node.id(), ex.getMessage());
    }

    @Test
    void testDefaultHandlerInputType() {
        assertEquals(DataMessage.class, proxy.inputType());
    }

    @Test
    void testDefaultHandlerForWitness() {
        proxy.handle(address, message);
        verify(messageStore).add(message);
        verify(node).sendInternal(connection.receiver(), message, TransportUtils.defaultOptions());
        assertArrayEquals(witnessSignature, message.witnessSignature().orElseThrow());
    }

    @Test
    void testDefaultHandlerCalledForReceiver() {
        when(node.id()).thenReturn(PeerUtils.receiverId());
        proxy.handle(address, message);
        verify(messageStore).add(message);
        verify(node, never()).sendInternal(connection.receiver(), message, TransportUtils.defaultOptions());
        assertTrue(message.witnessSignature().isEmpty());
    }

}