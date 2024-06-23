package com.github.arobie1992.clarinet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.core.*;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.impl.crypto.KeyProviders;
import com.github.arobie1992.clarinet.impl.crypto.Keys;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryAssessmentStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryKeyStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryMessageStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryPeerStore;
import com.github.arobie1992.clarinet.impl.netty.ConnectionIdSerializer;
import com.github.arobie1992.clarinet.impl.netty.NettyTransport;
import com.github.arobie1992.clarinet.impl.netty.PeerIdSerializer;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.impl.reputation.ProportionalReputationService;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageForward;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.reputation.TrustFilters;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Node sender, witness, receiver;
    private CountDownLatch witnessNotificationLatch;
    private CountDownLatch messageLatch;
    private CountDownLatch witnessCloseLatch;
    private CountDownLatch receiverCloseLatch;

    IntegrationTest() {
        var module = new SimpleModule();
        module.addSerializer(PeerId.class, new PeerIdSerializer());
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());
    }

    @BeforeEach
    void setUp() throws URISyntaxException, NoSuchAlgorithmException {
        sender = Nodes.newBuilder().id(PeerUtils.senderId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(PeerUtils.senderId(), TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .assessmentStore(new InMemoryAssessmentStore())
                .reputationService(new ProportionalReputationService())
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        sender.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        sender.keyStore().addKeyPair(sender.id(), Keys.generateKeyPair());
        sender.keyStore().addProvider(KeyProviders.Sha256RsaPublicKeyProvider());

        witness = Nodes.newBuilder().id(PeerUtils.witnessId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(PeerUtils.witnessId(), TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .assessmentStore(new InMemoryAssessmentStore())
                .reputationService(new ProportionalReputationService())
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        witness.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        witness.keyStore().addKeyPair(witness.id(), Keys.generateKeyPair());
        witness.keyStore().addProvider(KeyProviders.Sha256RsaPublicKeyProvider());

        receiver = Nodes.newBuilder().id(PeerUtils.receiverId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(PeerUtils.receiverId(), TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .assessmentStore(new InMemoryAssessmentStore())
                .reputationService(new ProportionalReputationService())
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        receiver.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        receiver.keyStore().addKeyPair(receiver.id(), Keys.generateKeyPair());
        receiver.keyStore().addProvider(KeyProviders.Sha256RsaPublicKeyProvider());

        witnessNotificationLatch = new CountDownLatch(1);
        messageLatch = new CountDownLatch(1);
        witnessCloseLatch = new CountDownLatch(1);
        receiverCloseLatch = new CountDownLatch(1);
    }

    record SendLatchHandler<T>(CountDownLatch latch, Class<T> inputType) implements SendHandler<T> {
        @Override
        public None<Void> handle(RemoteInformation remoteInformation, T message) {
            latch.countDown();
            return new None<>();
        }
    }

    private record RetrieveAddrsWitnessHandler(Node node) implements ExchangeHandler<WitnessRequest, WitnessResponse> {
        @Override
        public Some<WitnessResponse> handle(RemoteInformation remoteInformation, WitnessRequest message) {
            var neededPeers = new HashSet<PeerId>();
            if (needsAddress(message.sender())) {
                neededPeers.add(message.sender());
            }
            if (needsAddress(message.receiver())) {
                neededPeers.add(message.receiver());
            }
            node.requestPeers(remoteInformation.peer().id(), new PeersRequest(neededPeers), TransportUtils.defaultOptions())
                    .peers()
                    .forEach(node.peerStore()::save);
            var rejected = needsAddress(message.receiver());
            var reason = rejected ? "No address for receiver" : null;
            return new Some<>(new WitnessResponse(rejected, reason));
        }
        private boolean needsAddress(PeerId peerId) {
            return node.peerStore().find(peerId).map(p -> p.addresses().isEmpty()).orElse(true);
        }
        @Override
        public Class<WitnessRequest> inputType() {
            return WitnessRequest.class;
        }
    }

    @Test
    void testCooperative() throws InterruptedException, JsonProcessingException {
        sender.peerStore().save(asPeer(receiver));
        sender.peerStore().save(asPeer(witness));
        receiver.addWitnessNotificationHandler(new SendLatchHandler<>(witnessNotificationLatch, WitnessNotification.class));
        witness.addWitnessRequestHandler(new RetrieveAddrsWitnessHandler(witness));
        receiver.addReceiveHandler(new SendLatchHandler<>(messageLatch, DataMessage.class));

        // connection creation
        var connectionId = sender.connect(receiver.id(), new ConnectionOptions(), TransportUtils.defaultOptions());
        var expected = new TestConnection(connectionId, sender.id(), Optional.of(witness.id()), receiver.id(), Connection.Status.OPEN);
        // need latch to ensure test doesn't do verification before the witness notification handler has executed
        // if it waited all 5 seconds, something's probably wrong and we want to revisit this.
        assertTrue(witnessNotificationLatch.await(5, TimeUnit.SECONDS));
        verifyConnectionPresent(expected, sender);
        verifyConnectionPresent(expected, witness);
        verifyConnectionPresent(expected, receiver);

        // sending a message
        var data = Bytes.of(new byte[]{0, 1, 2, 3, 4});
        var messageId = sender.send(connectionId, data, TransportUtils.defaultOptions());
        // need latch to ensure test doesn't do verification before the receiver gets the message
        // if it waited all 5 seconds, something's probably wrong and we want to revisit this.
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        verifyMessage(sender, messageId, 0, data, MessageVerificationMode.SENDER_ONLY);
        verifyMessage(witness, messageId, 0, data, MessageVerificationMode.SENDER_AND_WITNESS);
        verifyMessage(receiver, messageId, 0, data, MessageVerificationMode.SENDER_AND_WITNESS);

        verifyAssessment(sender, witness.id(), messageId, Assessment.Status.NONE);
        verifyAssessment(sender, receiver.id(), messageId, Assessment.Status.NONE);
        verifyAssessment(witness, sender.id(), messageId, Assessment.Status.REWARD);
        verifyAssessment(witness, receiver.id(), messageId, Assessment.Status.NONE);
        verifyAssessment(receiver, sender.id(), messageId, Assessment.Status.REWARD);
        verifyAssessment(receiver, witness.id(), messageId, Assessment.Status.REWARD);

        // sender querying message
        var resp = sender.query(witness.id(), messageId, new TransportOptions());
        sender.processQueryResult(resp, new TransportOptions());
        verifyAssessment(sender, witness.id(), messageId, Assessment.Status.REWARD);
        verifyReputation(sender, witness.id(), 1);
        resp = sender.query(receiver.id(), messageId, new TransportOptions());
        sender.processQueryResult(resp, new TransportOptions());
        verifyAssessment(sender, receiver.id(), messageId, Assessment.Status.REWARD);
        verifyReputation(sender, receiver.id(), 1);

        // witness querying message
        resp = witness.query(sender.id(), messageId, new TransportOptions());
        witness.processQueryResult(resp, new TransportOptions());
        verifyAssessment(witness, sender.id(), messageId, Assessment.Status.REWARD);
        verifyReputation(witness, sender.id(), 1);
        resp = witness.query(receiver.id(), messageId, new TransportOptions());
        witness.processQueryResult(resp, new TransportOptions());
        verifyAssessment(witness, receiver.id(), messageId, Assessment.Status.REWARD);
        verifyReputation(witness, receiver.id(), 1);

        // receiver querying message
        resp = receiver.query(sender.id(), messageId, new TransportOptions());
        receiver.processQueryResult(resp, new TransportOptions());
        verifyAssessment(receiver, sender.id(), messageId, Assessment.Status.REWARD);
        verifyReputation(receiver, sender.id(), 1);
        resp = receiver.query(witness.id(), messageId, new TransportOptions());
        receiver.processQueryResult(resp, new TransportOptions());
        verifyAssessment(receiver, witness.id(), messageId, Assessment.Status.REWARD);
        verifyReputation(receiver, witness.id(), 1);

        witness.addCloseHandler(new SendLatchHandler<>(witnessCloseLatch, CloseRequest.class));
        receiver.addCloseHandler(new SendLatchHandler<>(receiverCloseLatch, CloseRequest.class));

        sender.close(connectionId, new CloseOptions(), new TransportOptions());
        assertTrue(witnessCloseLatch.await(5, TimeUnit.SECONDS));
        assertTrue(receiverCloseLatch.await(5, TimeUnit.SECONDS));
        var closedConn = new TestConnection(connectionId, sender.id(), Optional.of(witness.id()), receiver.id(), Connection.Status.CLOSED);
        verifyConnectionPresent(closedConn, sender);
        verifyConnectionPresent(closedConn, witness);
        verifyConnectionPresent(closedConn, receiver);
    }

    /*
     TODO items
     - receiver forwarding message to sender during send when sender signature is invalid
     - forwarding query
     - allowing configuration in which side picks the witness
     */

    @Disabled
    @Test
    void testMaliciousSender() {
        fail("implement");
    }

    @Test
    void testMaliciousWitness() throws NoSuchAlgorithmException, URISyntaxException, InterruptedException {
        witness = new MaliciousNode.MaliciuosNodeBuilder(new MaliciousNode.Configuration.Builder().witnessAlterData(true).build())
                .id(PeerUtils.witnessId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(PeerUtils.witnessId(), TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .assessmentStore(new InMemoryAssessmentStore())
                .reputationService(new ProportionalReputationService())
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        witness.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        witness.keyStore().addKeyPair(witness.id(), Keys.generateKeyPair());
        witness.keyStore().addProvider(KeyProviders.Sha256RsaPublicKeyProvider());

        sender.peerStore().save(asPeer(receiver));
        sender.peerStore().save(asPeer(witness));
        receiver.addWitnessNotificationHandler(new SendLatchHandler<>(witnessNotificationLatch, WitnessNotification.class));
        witness.addWitnessRequestHandler(new RetrieveAddrsWitnessHandler(witness));
        receiver.addReceiveHandler(new SendLatchHandler<>(messageLatch, DataMessage.class));

        // connection creation
        var connectionId = sender.connect(receiver.id(), new ConnectionOptions(), TransportUtils.defaultOptions());
        // need latch to ensure test doesn't do verification before the witness notification handler has executed
        // if it waited all 5 seconds, something's probably wrong and we want to revisit this.
        assertTrue(witnessNotificationLatch.await(5, TimeUnit.SECONDS));

        var forwardLatch = new CountDownLatch(1);
        sender.addMessageForwardHandler(new SendLatchHandler<>(forwardLatch, MessageForward.class));

        // sending a message
        var data = Bytes.of(new byte[]{0, 1, 2, 3, 4});
        var messageId = sender.send(connectionId, data, TransportUtils.defaultOptions());
        // need latch to ensure test doesn't do verification before the receiver gets the message
        // if it waited all 5 seconds, something's probably wrong and we want to revisit this.
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        assertTrue(forwardLatch.await(5, TimeUnit.SECONDS));

        verifyAssessment(sender, witness.id(), messageId, Assessment.Status.STRONG_PENALTY);
        verifyAssessment(sender, receiver.id(), messageId, Assessment.Status.NONE);
        verifyAssessment(receiver, sender.id(), messageId, Assessment.Status.WEAK_PENALTY);
        verifyAssessment(receiver, witness.id(), messageId, Assessment.Status.WEAK_PENALTY);

        // sender querying message
        var resp = sender.query(witness.id(), messageId, new TransportOptions());
        sender.processQueryResult(resp, new TransportOptions());
        verifyAssessment(sender, witness.id(), messageId, Assessment.Status.STRONG_PENALTY);
        verifyReputation(sender, witness.id(), 0.25);
        resp = sender.query(receiver.id(), messageId, new TransportOptions());
        sender.processQueryResult(resp, new TransportOptions());
        verifyAssessment(sender, receiver.id(), messageId, Assessment.Status.WEAK_PENALTY);
        verifyReputation(sender, receiver.id(), 0.5);

        // receiver querying message
        resp = receiver.query(sender.id(), messageId, new TransportOptions());
        receiver.processQueryResult(resp, new TransportOptions());
        verifyAssessment(receiver, sender.id(), messageId, Assessment.Status.WEAK_PENALTY);
        verifyReputation(receiver, sender.id(), 0.5);
        resp = receiver.query(witness.id(), messageId, new TransportOptions());
        receiver.processQueryResult(resp, new TransportOptions());
        verifyAssessment(receiver, witness.id(), messageId, Assessment.Status.WEAK_PENALTY);
        verifyReputation(receiver, witness.id(),  0.5);
    }

    @Disabled
    @Test
    void testMaliciousReceiver() {
        fail("implement");
    }

    @Disabled
    @Test
    void testMaliciousSenderAndWitness() {
        fail("implement");
    }

    @Disabled
    @Test
    void testMaliciousSenderAndReceiver() {
        fail("implement");
    }

    @Disabled
    @Test
    void testMaliciousWitnessAndReceiver() {
        fail("implement");
    }

    @Disabled
    @Test
    void testConnectFailsToReachReceiver() {
        fail("implement");
    }

    @Disabled
    @Test
    void testConnectFailsToReachSender() {
        fail("implement");
    }

    @Disabled
    @Test
    void testConnectNoTrustedWitnessesWitness() {
        fail("implement");
    }

    @Disabled
    @Test
    void testConnectFailsToNotifyOfWitness() {
        fail("implement");
    }

    @Disabled
    @Test
    void testWitnessDropsMessage() {
        fail("implement");
    }

    @Disabled
    @Test
    void testReceiverDropsMessage() {
        fail("implement");
    }

    @Disabled
    @Test
    void testCloseFailsAtReceiver() {
        fail("implement");
    }

    @Disabled
    @Test
    void testCloseFailsAtWitness() {
        fail("implement");
    }

    @Disabled
    @Test
    void testCloseFailsAtSender() {
        fail("implement");
    }

    @AfterEach
    void teardown() {
        sender.transport().shutdown();
        witness.transport().shutdown();
        receiver.transport().shutdown();
    }

    private Peer asPeer(Node node) {
        var peer = new Peer(node.id());
        peer.addresses().addAll(node.transport().addresses());
        return peer;
    }

    void verifyConnectionPresent(TestConnection expected, Node node) {
        try(var ref = assertDoesNotThrow(() -> node.findConnection(expected.id()))) {
            switch (ref) {
                case Connection.Readable(Connection connection) -> expected.assertMatches(connection);
                case Connection.Absent() -> fail("connection not found");
            }
        }
    }

    private enum MessageVerificationMode {
        SENDER_ONLY,
        SENDER_AND_WITNESS
    }

    private void verifyMessage(
            Node node,
            MessageId messageId,
            @SuppressWarnings("SameParameterValue") long seqNo,
            Bytes data,
            MessageVerificationMode mode
    ) throws JsonProcessingException {
        try(var ref = node.findConnection(messageId.connectionId())) {
            if(!(ref instanceof Connection.Readable(Connection connection))) {
                fail("connection not found");
                throw new IllegalStateException("unreachable");
            }
            var messageOpt = node.messageStore().find(messageId);
            assertTrue(messageOpt.isPresent());
            var message = messageOpt.get();
            assertEquals(data, message.data());
            assertEquals(seqNo, message.messageId().sequenceNumber());
            Collection<PublicKey> pubKeys;
            Bytes encoded;
            switch(mode) {
                case SENDER_AND_WITNESS:
                    pubKeys = node.keyStore().findPublicKeys(connection.witness().orElseThrow());
                    encoded = Bytes.of(objectMapper.writeValueAsBytes(message.witnessParts()));
                    verifyMessage(pubKeys, encoded, message.witnessSignature().orElseThrow());
                case SENDER_ONLY:
                    pubKeys = node.keyStore().findPublicKeys(connection.sender());
                    encoded = Bytes.of(objectMapper.writeValueAsBytes(message.senderParts()));
                    verifyMessage(pubKeys, encoded, message.senderSignature().orElseThrow());
                    break;
            }
        }
    }

    private void verifyMessage(Collection<PublicKey> pubKeys, Bytes data, Bytes signature) {
        var key = pubKeys.iterator().next();
        assertTrue(key.verify(data, signature));
    }

    private void verifyAssessment(Node node, PeerId peerId, MessageId messageId, Assessment.Status expected) {
        var assessment = node.assessmentStore().find(peerId, messageId);
        assertEquals(expected, assessment.status());
    }

    private void verifyReputation(Node node, PeerId peerId, double expected) {
        var rep = node.reputationService().get(peerId);
        assertEquals(expected, rep);
    }
}