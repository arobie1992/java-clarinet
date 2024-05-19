package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.core.Connection;
import com.github.arobie1992.clarinet.core.ConnectionOptions;
import com.github.arobie1992.clarinet.core.Node;
import com.github.arobie1992.clarinet.core.Nodes;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryPeerStore;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.impl.tcp.TcpTransport;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

class IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    private record Context(Node sender, Node witness, Node receiver) {}

    private Context ctx;

    @BeforeEach
    void setUp() throws URISyntaxException {
        final Node sender = Nodes.newBuilder().id(PeerUtils.senderId())
                .peerStore(new InMemoryPeerStore())
                .transport(new TcpTransport())
                .build();
        final Node witness = Nodes.newBuilder().id(PeerUtils.witnessId())
                .peerStore(new InMemoryPeerStore())
                .transport(new TcpTransport())
                .build();
        final Node receiver = Nodes.newBuilder().id(PeerUtils.receiverId())
                .peerStore(new InMemoryPeerStore())
                .transport(new TcpTransport())
                .build();
        receiver.transport().add(new UriAddress(new URI("tcp://localhost:0")));
        ctx = new Context(sender, witness, receiver);
    }

    @Test
    void testCooperative() throws Exception {
        ctx.sender.peerStore().save(asPeer(ctx.receiver));
        ctx.sender.peerStore().save(asPeer(ctx.witness));
        var connectionId = ctx.sender.connect(ctx.receiver.id(), new ConnectionOptions(), TransportUtils.defaultOptions());
        var expected = new TestConnection(
                connectionId,
                ctx.sender().id(),
                Optional.of(ctx.witness.id()),
                ctx.receiver.id(),
                Connection.Status.OPEN
        );
        verifyConnectionPresent(expected, ctx.sender);
        verifyConnectionPresent(expected, ctx.witness);
        verifyConnectionPresent(expected, ctx.receiver);
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
        Consumer<Address> closeAddr = a -> {
            try {
                ctx.sender.transport().remove(a);
            } catch(Exception e) {
                log.warn("Failed to close address: {}", a, e);
            }
        };
        ctx.sender.transport().addresses().forEach(closeAddr);
        ctx.witness.transport().addresses().forEach(closeAddr);
        ctx.receiver.transport().addresses().forEach(closeAddr);
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