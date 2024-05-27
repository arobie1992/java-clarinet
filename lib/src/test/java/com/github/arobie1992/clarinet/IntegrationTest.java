package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.core.*;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryKeyStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryMessageStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryPeerStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryReputationStore;
import com.github.arobie1992.clarinet.impl.netty.NettyTransport;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.reputation.TrustFilters;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.testutils.TestReputation;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    private Node sender, witness, receiver;
    private CountDownLatch sendLatch;

    @BeforeEach
    void setUp() throws URISyntaxException {
        sender = Nodes.newBuilder().id(PeerUtils.senderId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .reputationStore(new InMemoryReputationStore(TestReputation::new))
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        witness = Nodes.newBuilder().id(PeerUtils.witnessId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .reputationStore(new InMemoryReputationStore(TestReputation::new))
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        witness.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        receiver = Nodes.newBuilder().id(PeerUtils.receiverId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(TransportUtils.defaultOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .reputationStore(new InMemoryReputationStore(TestReputation::new))
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        receiver.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        sendLatch = new CountDownLatch(1);
    }

    @Test
    void testCooperative() throws InterruptedException {
        sender.peerStore().save(asPeer(receiver));
        sender.peerStore().save(asPeer(witness));
        receiver.addWitnessNotificationHandler(new SendHandler<>() {
            @Override
            public None<Void> handle(Address remoteAddress, WitnessNotification message) {
                sendLatch.countDown();
                return null;
            }

            @Override
            public Class<WitnessNotification> inputType() {
                return WitnessNotification.class;
            }
        });

        // connection creation
        var connectionId = sender.connect(receiver.id(), new ConnectionOptions(), TransportUtils.defaultOptions());
        var expected = new TestConnection(connectionId, sender.id(), Optional.of(witness.id()), receiver.id(), Connection.Status.OPEN);
        // need latch to ensure test doesn't do verification before the witness notification handler has executed
        sendLatch.await();
        verifyConnectionPresent(expected, sender);
        verifyConnectionPresent(expected, witness);
        verifyConnectionPresent(expected, receiver);

        // sending a message
        var data = new byte[]{0, 1, 2, 3, 4};
        var messageId = sender.send(connectionId, data);
        var messageOpt = sender.messageStore().find(messageId);
        assertTrue(messageOpt.isPresent());
        var message = messageOpt.get();
        assertEquals(new String(data), message.data());
        assertEquals(connectionId, message.messageId().connectionId());
        assertEquals(0, message.messageId().sequenceNumber());
        var pubKeys = sender.keyStore().findPublicKeys(sender.id());
        assertFalse(pubKeys.isEmpty());
//        assertTrue(verifySender(message, pubKeyOpt.get()));

        fail("test for witness and receiver");
        fail("Test reputation, and querying");

        // sender querying message

        // witness querying message

        // receiver querying message
    }

    @Test
    void testMaliciousSender() {
        fail("implement");
    }

    @Test
    void testMaliciousWitness() {
        fail("implement");
    }

    @Test
    void testMaliciousReceiver() {
        fail("implement");
    }

    @Test
    void testMaliciousSenderAndWitness() {
        fail("implement");
    }

    @Test
    void testMaliciousSenderAndReceiver() {
        fail("implement");
    }

    @Test
    void testMaliciousWitnessAndReceiver() {
        fail("implement");
    }

    @Test
    void testConnectFailsToReachReceiver() {
        fail("implement");
    }

    @Test
    void testConnectFailsToReachSender() {
        fail("implement");
    }

    @Test
    void testConnectNoTrustedWitnessesWitness() {
        fail("implement");
    }

    @Test
    void testConnectFailsToNotifyOfWitness() {
        fail("implement");
    }

    @Test
    void testWitnessDropsMessage() {
        fail("implement");
    }

    @Test
    void testReceiverDropsMessage() {
        fail("implement");
    }

    @Test
    void testCloseFailsAtReceiver() {
        fail("implement");
    }

    @Test
    void testCloseFailsAtWitness() {
        fail("implement");
    }

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

}