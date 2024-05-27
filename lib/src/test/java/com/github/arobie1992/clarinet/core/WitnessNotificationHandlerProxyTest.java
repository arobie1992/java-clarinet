package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WitnessNotificationHandlerProxyTest {

    private final WitnessNotification witnessNotification = new WitnessNotification(ConnectionId.random(), PeerUtils.witnessId());

    private SendHandler<WitnessNotification> handler;
    private ConnectionStore connectionStore;
    private WitnessNotificationHandlerProxy handlerProxy;
    private ConnectionImpl connection;
    private TestConnection expected;

    @BeforeEach
    public void setUp() {
        //noinspection unchecked
        handler = (SendHandler<WitnessNotification>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        handlerProxy = new WitnessNotificationHandlerProxy(handler, connectionStore);
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
    }

    @Test
    void testUserHandler() {
        handlerProxy.handle(AddressUtils.defaultAddress(), witnessNotification);
        verify(handler).handle(AddressUtils.defaultAddress(), witnessNotification);
        expected.assertMatches(connection);
    }

    @Test
    void testNullUserHandler() {
        handlerProxy = new WitnessNotificationHandlerProxy(null, connectionStore);
        handlerProxy.handle(AddressUtils.defaultAddress(), witnessNotification);
        expected.assertMatches(connection);
    }

    @Test
    void testNoConnection() {
        when(connectionStore.findForWrite(witnessNotification.connectionId())).thenReturn(new Connection.Absent());
        var ex = assertThrows(NoSuchConnectionException.class, () -> handlerProxy.handle(AddressUtils.defaultAddress(), witnessNotification));
        assertEquals(witnessNotification.connectionId(), ex.connectionId());
    }

    @Test
    void testDefaultHandlerInputType() {
        handlerProxy = assertDoesNotThrow(() -> new WitnessNotificationHandlerProxy(null, connectionStore));
        assertEquals(WitnessNotification.class, handlerProxy.inputType());
    }

    @Test
    void testConnectionNotAwaitingWitness() {
        connection.setStatus(Connection.Status.REQUESTING_RECEIVER);
        var ex = assertThrows(
                UnsupportedOperationException.class,
                () -> handlerProxy.handle(AddressUtils.defaultAddress(), witnessNotification)
        );
        assertEquals("Connection " + witnessNotification.connectionId() + " is not awaiting witness.", ex.getMessage());
    }

}