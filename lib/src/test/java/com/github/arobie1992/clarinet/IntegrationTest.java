package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.core.Connection;
import com.github.arobie1992.clarinet.core.ConnectionOptions;
import com.github.arobie1992.clarinet.core.Node;
import com.github.arobie1992.clarinet.core.Nodes;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryPeerStore;
import com.github.arobie1992.clarinet.impl.netty.NettyTransport;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

class IntegrationTest {

    private Node sender, witness, receiver;

    @BeforeEach
    void setUp() throws URISyntaxException {
        sender = Nodes.newBuilder().id(PeerUtils.senderId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(TransportUtils.defaultOptions()))
                .build();
        witness = Nodes.newBuilder().id(PeerUtils.witnessId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(TransportUtils.defaultOptions()))
                .build();
        receiver = Nodes.newBuilder().id(PeerUtils.receiverId())
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(TransportUtils.defaultOptions()))
                .build();
        receiver.transport().add(new UriAddress(new URI("tcp://localhost:0")));
    }

    @Test
    void testCooperative() throws Exception {
        sender.peerStore().save(asPeer(receiver));
        sender.peerStore().save(asPeer(witness));
        var connectionId = sender.connect(receiver.id(), new ConnectionOptions(), TransportUtils.defaultOptions());
        var expected = new TestConnection(connectionId, sender.id(), Optional.of(witness.id()), receiver.id(), Connection.Status.OPEN);
        verifyConnectionPresent(expected, sender);
        verifyConnectionPresent(expected, witness);
        verifyConnectionPresent(expected, receiver);
        fail("Test sending messages, reputation, and querying");
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
        ((NettyTransport) sender.transport()).close();
        ((NettyTransport) witness.transport()).close();
        ((NettyTransport) receiver.transport()).close();
    }

    private Peer asPeer(Node node) {
        var peer = new Peer(node.id());
        peer.addresses().addAll(node.transport().addresses());
        return peer;
    }

    void verifyConnectionPresent(TestConnection expected, Node node) throws Exception {
        try(var ref = assertDoesNotThrow(() -> node.findConnection(expected.id()))) {
            switch (ref) {
                case Connection.Readable(Connection connection) -> expected.assertMatches(connection);
                case Connection.Absent() -> fail("connection not found");
            }
        }
    }

}