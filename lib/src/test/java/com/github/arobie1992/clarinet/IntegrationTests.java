package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.data.MessageID;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class IntegrationTests {
    @Test
    public void test() {
        var sender = new SimpleNodeBuilder().build();
        var witness = new SimpleNodeBuilder().build();
        var receiver = new SimpleNodeBuilder().build();

        sender.updatePeer(witness.self());
        sender.updatePeer(receiver.self());

        var connectionId = sender.connect(receiver.self(), new ConnectionOptions(), new TransportOptions(Optional.empty(), Optional.empty()));
        var messageId = sender.send(connectionId, new TransportOptions(Optional.empty(), Optional.empty()), "Test message".getBytes(StandardCharsets.UTF_8));
        queryCooperativePeer(sender, messageId, receiver);
        queryCooperativePeer(sender, messageId, witness);

        queryCooperativePeer(witness, messageId, sender);
        queryCooperativePeer(witness, messageId, receiver);

        queryCooperativePeer(receiver, messageId, sender);
        queryCooperativePeer(receiver, messageId, witness);
    }

    private void queryCooperativePeer(Node node, MessageID messageID, Node peer) {
        node.query(messageID, peer.self());
        var reputation = node.getReputation(peer.self());
        Assertions.assertEquals(1, reputation.value());
    }
}
