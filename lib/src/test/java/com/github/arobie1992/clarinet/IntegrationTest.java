package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.core.Connection;
import com.github.arobie1992.clarinet.core.ConnectionOptions;
import com.github.arobie1992.clarinet.core.Node;
import com.github.arobie1992.clarinet.core.Nodes;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryPeerStore;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.TestConnection;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

class IntegrationTest {

    private record Context(Node sender, Node witness, Node receiver) {}

    private Context ctx;


    @BeforeEach
    void setUp() throws URISyntaxException {
        final Node sender = Nodes.newBuilder().id(PeerUtils.senderId()).peerStore(new InMemoryPeerStore()).build();
        final Node witness = Nodes.newBuilder().id(PeerUtils.witnessId()).peerStore(new InMemoryPeerStore()).build();
        final Node receiver = Nodes.newBuilder().id(PeerUtils.receiverId()).peerStore(new InMemoryPeerStore()).build();
        receiver.addresses().add(UriAddress.from(new URI("tcp://localhost")));
        ctx = new Context(sender, witness, receiver);
    }

    @Test
    void testConnect() throws Exception {
        ctx.sender.peerStore().save(asPeer(ctx.witness));
        var connectionId = ctx.sender.connect(ctx.receiver.id(), new ConnectionOptions(), new TransportOptions());
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
    }

    private Peer asPeer(Node node) {
        var peer = new Peer(node.id());
        peer.addresses().addAll(node.addresses());
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