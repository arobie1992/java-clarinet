package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.message.*;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.reputation.AssessmentStore;
import com.github.arobie1992.clarinet.reputation.ReputationService;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
        var summary = new MessageSummary(messageId, Bytes.of(new byte[]{99,44,55}), "SHA-256", Bytes.of(new byte[]{91,23}));
        forward = new MessageForward(summary, Bytes.of(new byte[]{65}));
    }

    private SendHandler<MessageForward> userHandler;
    private ConnectionStore connectionStore;
    private SimpleNode node;
    private MessageForwardHandlerProxy proxy;
    private AssessmentStore assessmentStore;
    private Assessment recAsmt;
    private Assessment witAsmt;
    private MessageStore messageStore;
    private DataMessage message;

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        userHandler = (SendHandler<MessageForward>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(SimpleNode.class);
        proxy = new MessageForwardHandlerProxy(userHandler, connectionStore, node);

        assessmentStore = mock(AssessmentStore.class);
        when(node.assessmentStore()).thenReturn(assessmentStore);
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
                forward.summary().hash(),
                PeerUtils.witnessId(),
                forward.summary().witnessSignature())
        ).thenReturn(true);

        message = mock(DataMessage.class);
        when(messageStore.find(forward.summary().messageId())).thenReturn(Optional.of(message));
        var witnessParts = mock(DataMessage.WitnessParts.class);
        when(message.witnessParts()).thenReturn(witnessParts);

        recAsmt = new Assessment(PeerUtils.receiverId(), forward.summary().messageId(), Assessment.Status.NONE);
        when(assessmentStore.find(PeerUtils.receiverId(), forward.summary().messageId())).thenReturn(recAsmt);
        witAsmt = new Assessment(PeerUtils.witnessId(), forward.summary().messageId(), Assessment.Status.NONE);
        when(assessmentStore.find(PeerUtils.witnessId(), forward.summary().messageId())).thenReturn(witAsmt);
        when(node.hash(message.witnessParts(), forward.summary().hashAlgorithm())).thenReturn(forward.summary().hash());

        var repService = mock(ReputationService.class);
        when(node.reputationService()).thenReturn(repService);
    }

    @Test
    void testNoConnection() {
        when(connectionStore.findForRead(forward.summary().messageId().connectionId())).thenReturn(new Connection.Absent());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(assessmentStore, userHandler);
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
        verifyNoInteractions(assessmentStore, userHandler);
    }

    @Test
    void testNotSender() {
        when(node.id()).thenReturn(PeerUtils.witnessId());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(assessmentStore, userHandler);
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
        verifyNoInteractions(assessmentStore, userHandler);
    }

    @Test
    void testWitSigInvalid() {
        when(node.checkSignatureHash(
                forward.summary().hash(),
                PeerUtils.witnessId(),
                forward.summary().witnessSignature())
        ).thenReturn(false);
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(userHandler);
        verify(assessmentStore).save(eq(recAsmt.updateStatus(Assessment.Status.STRONG_PENALTY)), any());
    }

    @Test
    void testNoMessage() {
        when(messageStore.find(forward.summary().messageId())).thenReturn(Optional.empty());
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(assessmentStore, userHandler);
    }

    @Test
    void testHashIncorrect() {
        var hash = Bytes.of(new byte[]{1,1,1,1});
        assertNotEquals(forward.summary().hash(), hash);
        when(node.hash(message.witnessParts(), forward.summary().hashAlgorithm())).thenReturn(hash);
        proxy.handle(remoteInformation, forward);
        verify(assessmentStore).save(eq(witAsmt.updateStatus(Assessment.Status.STRONG_PENALTY)), any());
    }

    @Test
    void testHashCorrect() {
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(assessmentStore);
        verify(userHandler).handle(remoteInformation, forward);
    }

    @Test
    void testHashCorrectDefaultHandler() {
        proxy = new MessageForwardHandlerProxy(null, connectionStore, node);
        proxy.handle(remoteInformation, forward);
        verifyNoInteractions(assessmentStore);
    }

    @Test
    void testInputTypeDefaultHandler() {
        proxy = new MessageForwardHandlerProxy(null, connectionStore, node);
        assertEquals(MessageForward.class, proxy.inputType());
    }

}