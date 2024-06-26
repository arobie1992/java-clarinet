package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryForwardHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.witnessId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);
    private final MessageDetails messageDetails = new MessageDetails(messageId, Bytes.of(new byte[]{2,4,6,8}));
    private final QueryResponse queryResponse = new QueryResponse(messageDetails, Bytes.of(new byte[]{9,8,7}), "SHA-256");
    private final QueryForward queryForward = new QueryForward(PeerUtils.senderId(), queryResponse, Bytes.of(new byte[]{1,1,2,3,5}));
    private final Assessment forwarderAssessment = new Assessment(PeerUtils.witnessId(), messageId, Assessment.Status.NONE);
    private final Assessment queriedPeerAssessment = new Assessment(PeerUtils.receiverId(), messageId, Assessment.Status.NONE);
    private final DataMessage dataMessage = new DataMessage(messageId, Bytes.of(new byte[]{3,1,4,1,5,9}));

    private SendHandler<QueryForward> userHandler;
    private ConnectionStore connectionStore;
    private SimpleNode node;
    private QueryForwardHandlerProxy proxy;
    private AssessmentStore assessmentStore;
    private MessageStore messageStore;
    private ConnectionImpl connection;

    QueryForwardHandlerProxyTest() {
        dataMessage.setSenderSignature(Bytes.of(new byte[]{6,33,71}));
        dataMessage.setWitnessSignature(Bytes.of(new byte[]{6,33,72}));
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        //noinspection unchecked
        userHandler = (SendHandler<QueryForward>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(SimpleNode.class);
        proxy = new QueryForwardHandlerProxy(userHandler, connectionStore, node);

        assessmentStore = mock(AssessmentStore.class);
        when(node.assessmentStore()).thenReturn(assessmentStore);
        when(assessmentStore.find(remoteInformation.peer().id(), messageId)).thenReturn(forwarderAssessment);
        when(assessmentStore.find(queryForward.queriedPeer(), messageId)).thenReturn(queriedPeerAssessment);

        var reputationService = mock(ReputationService.class);
        when(node.reputationService()).thenReturn(reputationService);

        when(node.checkSignature(queryForward.queryResponse(), remoteInformation.peer().id(), queryForward.signature()))
                .thenReturn(true);
        when(node.checkSignature(queryResponse.messageDetails(), queryForward.queriedPeer(), queryResponse.signature()))
                .thenReturn(true);

        connection = new ConnectionImpl(messageId.connectionId(), PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        connection.lock.writeLock().lock();
        connection.setWitness(PeerUtils.witnessId());
        connection.lock.writeLock().unlock();
        connection.lock.readLock().lock();
        when(connectionStore.findForRead(messageId.connectionId())).thenReturn(new Connection.Readable(connection));

        messageStore = mock(MessageStore.class);
        when(node.messageStore()).thenReturn(messageStore);
        when(messageStore.find(messageId)).thenReturn(Optional.of(dataMessage));
    }

    @Test
    void testCheckSigThrowsJsonException() throws JsonProcessingException {
        when(node.checkSignature(queryForward.queryResponse(), remoteInformation.peer().id(), queryForward.signature()))
                .thenThrow(JsonProcessingException.class);
        var ex = assertThrows(UncheckedIOException.class, () -> proxy.handle(remoteInformation, queryForward));
        assertNotNull(ex.getCause());
        assertEquals(JsonProcessingException.class, ex.getCause().getClass());
    }

    @Test
    void testInvalidForwarderSig() throws JsonProcessingException {
        when(node.checkSignature(queryForward.queryResponse(), remoteInformation.peer().id(), queryForward.signature()))
                .thenReturn(false);
        proxy.handle(remoteInformation, queryForward);
        verify(assessmentStore).save(eq(forwarderAssessment.updateStatus(Assessment.Status.STRONG_PENALTY)), any());
        verify(userHandler).handle(remoteInformation, queryForward);
    }

    @Test
    void testInvalidQueriedPeerSig() throws JsonProcessingException {
        when(node.checkSignature(queryResponse.messageDetails(), queryForward.queriedPeer(), queryResponse.signature()))
                .thenReturn(false);
        proxy.handle(remoteInformation, queryForward);
        verify(assessmentStore).save(eq(forwarderAssessment.updateStatus(Assessment.Status.STRONG_PENALTY)), any());
        verify(userHandler).handle(remoteInformation, queryForward);
    }

    @Test
    void testNoConnection() {
        when(connectionStore.findForRead(messageId.connectionId())).thenReturn(new Connection.Absent());
        var resp = assertDoesNotThrow(() -> proxy.handle(remoteInformation, queryForward));
        assertNotNull(resp);
        verify(userHandler, never()).handle(any(), any());
    }

    @Test
    void testNoMessage() {
        when(messageStore.find(messageId)).thenReturn(Optional.empty());
        var resp = assertDoesNotThrow(() -> proxy.handle(remoteInformation, queryForward));
        assertNotNull(resp);
        verify(userHandler).handle(remoteInformation, queryForward);
    }

    @Test
    void testHashesNotMatchDirectCommunication() {
        when(node.directCommunication(queryForward.queriedPeer(), connection.participants())).thenReturn(true);
        var diff = Bytes.of(new byte[]{17});
        assertNotEquals(queryResponse.signature(), diff);
        when(node.hash(dataMessage.witnessParts(), queryResponse.hashAlgorithm())).thenReturn(diff);
        proxy.handle(remoteInformation, queryForward);
        verify(assessmentStore).save(eq(queriedPeerAssessment.updateStatus(Assessment.Status.STRONG_PENALTY)), any());
        verify(userHandler).handle(remoteInformation, queryForward);
    }

    @Test
    void testHashesNotMatchIndirectCommunication() {
        when(node.directCommunication(remoteInformation.peer().id(), connection.participants())).thenReturn(false);
        when(node.getOtherParticipant(connection.participants(), queryForward.queriedPeer())).thenReturn(remoteInformation.peer().id());
        var diff = Bytes.of(new byte[]{17});
        assertNotEquals(queryResponse.signature(), diff);
        when(node.hash(dataMessage.witnessParts(), queryResponse.hashAlgorithm())).thenReturn(diff);
        proxy.handle(remoteInformation, queryForward);
        verify(assessmentStore).save(eq(forwarderAssessment.updateStatus(Assessment.Status.WEAK_PENALTY)), any());
        verify(assessmentStore).save(eq(queriedPeerAssessment.updateStatus(Assessment.Status.WEAK_PENALTY)), any());
        verify(userHandler).handle(remoteInformation, queryForward);
    }

    @Test
    void testHashesMatch() {
        when(node.hash(dataMessage.witnessParts(), queryResponse.hashAlgorithm())).thenReturn(queryResponse.messageDetails().messageHash());
        proxy.handle(remoteInformation, queryForward);
        verify(assessmentStore).save(eq(queriedPeerAssessment.updateStatus(Assessment.Status.REWARD)), any());
        verify(userHandler).handle(remoteInformation, queryForward);
    }

    @Test
    void testHashesMatchDefaultHandler() {
        proxy = new QueryForwardHandlerProxy(null, connectionStore, node);
        when(node.hash(dataMessage.witnessParts(), queryResponse.hashAlgorithm())).thenReturn(queryResponse.messageDetails().messageHash());
        proxy.handle(remoteInformation, queryForward);
        verify(assessmentStore).save(eq(queriedPeerAssessment.updateStatus(Assessment.Status.REWARD)), any());
    }

    @Test
    void testDefaultHandlerInputType() {
        proxy = new QueryForwardHandlerProxy(null, connectionStore, node);
        assertEquals(QueryForward.class, proxy.inputType());
    }

}