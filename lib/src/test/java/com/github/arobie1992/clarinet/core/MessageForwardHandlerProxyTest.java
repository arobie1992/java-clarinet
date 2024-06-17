package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.message.*;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.github.arobie1992.clarinet.testutils.ArgumentMatcherUtils.byteArrayEq;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageForwardHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.receiverId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final MessageForward forward;

    MessageForwardHandlerProxyTest() {
        var messageId = new MessageId(ConnectionId.random(), 0);
        var summary = new MessageSummary(messageId, new byte[]{99,44,55}, "SHA-256", new byte[]{91,23});
        forward = new MessageForward(summary, new byte[]{65});
    }

    private SendHandler<MessageForward> userHandler;
    private ConnectionStore connectionStore;
    private SimpleNode node;
    private MessageForwardHandlerProxy proxy;
    private ReputationStore reputationStore;
    private Reputation recRep;
    private Reputation witRep;
    private MessageStore messageStore;
    private DataMessage message;

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        userHandler = (SendHandler<MessageForward>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(SimpleNode.class);
        proxy = new MessageForwardHandlerProxy(userHandler, connectionStore, node);

        reputationStore = mock(ReputationStore.class);
        when(node.reputationStore()).thenReturn(reputationStore);
        var connection = new ConnectionImpl(
                forward.summary().messageId().connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.OPEN
        );
        connection.lock.writeLock().lock();
        connection.setWitness(PeerUtils.witnessId());
        connection.lock.writeLock().unlock();
        connection.lock.readLock().lock();
        when(connectionStore.findForRead(forward.summary().messageId().connectionId())).thenReturn(new Connection.Readable(connection));

        when(node.id()).thenReturn(PeerUtils.senderId());

        messageStore = mock(MessageStore.class);
        when(node.messageStore()).thenReturn(messageStore);

        when(node.checkSignatureHash(
                byteArrayEq(forward.summary().hash()),
                eq(PeerUtils.witnessId()),
                byteArrayEq(forward.summary().witnessSignature()))
        ).thenReturn(true);

        message = mock(DataMessage.class);
        when(messageStore.find(forward.summary().messageId())).thenReturn(Optional.of(message));
        var witnessParts = mock(DataMessage.WitnessParts.class);
        when(message.witnessParts()).thenReturn(witnessParts);

        recRep = mock(Reputation.class);
        when(reputationStore.find(PeerUtils.receiverId())).thenReturn(recRep);
        witRep = mock(Reputation.class);
        when(reputationStore.find(PeerUtils.witnessId())).thenReturn(witRep);
        when(node.hash(message.witnessParts(), forward.summary().hashAlgorithm())).thenReturn(forward.summary().hash());
    }

    @Test
    void testNoConnection() {
        when(connectionStore.findForRead(forward.summary().messageId().connectionId())).thenReturn(new Connection.Absent());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore, userHandler);
    }

    @Test
    void testNotFromReceiver() {
        var connection = new ConnectionImpl(
                forward.summary().messageId().connectionId(),
                PeerUtils.senderId(),
                PeerUtils.witnessId(),
                Connection.Status.OPEN
        );
        when(connectionStore.findForRead(forward.summary().messageId().connectionId())).thenReturn(new Connection.Readable(connection));
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore, userHandler);
    }

    @Test
    void testNotSender() {
        when(node.id()).thenReturn(PeerUtils.witnessId());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore, userHandler);
    }

    @Test
    void testNullWitness() {
        var connection = new ConnectionImpl(
                forward.summary().messageId().connectionId(),
                PeerUtils.senderId(),
                PeerUtils.receiverId(),
                Connection.Status.OPEN
        );
        when(connectionStore.findForRead(forward.summary().messageId().connectionId())).thenReturn(new Connection.Readable(connection));
        assertTrue(connection.witness().isEmpty());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore, userHandler);
    }

    @Test
    void testWitSigInvalid() {
        when(node.checkSignatureHash(
                byteArrayEq(forward.summary().hash()),
                eq(PeerUtils.witnessId()),
                byteArrayEq(forward.summary().witnessSignature()))
        ).thenReturn(false);
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(userHandler);
        verify(recRep).strongPenalize();
        verify(reputationStore).save(recRep);
    }

    @Test
    void testNoMessage() {
        when(messageStore.find(forward.summary().messageId())).thenReturn(Optional.empty());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore, userHandler);
    }

    @Test
    void testHashIncorrect() {
        var hash = new byte[]{1,1,1,1};
        assertFalse(Arrays.equals(forward.summary().hash(), hash));
        when(node.hash(message.witnessParts(), forward.summary().hashAlgorithm())).thenReturn(hash);
        proxy.handle(remoteInformation, forward);
        verify(witRep).strongPenalize();
        verify(reputationStore).save(witRep);
    }

    @Test
    void testHashCorrect() {
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore);
        verify(userHandler).handle(remoteInformation, forward);
    }

    @Test
    void testHashCorrectDefaultHandler() {
        proxy = new MessageForwardHandlerProxy(null, connectionStore, node);
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(reputationStore);
    }

    @Test
    void testInputTypeDefaultHandler() {
        proxy = new MessageForwardHandlerProxy(null, connectionStore, node);
        assertEquals(MessageForward.class, proxy.inputType());
    }

}