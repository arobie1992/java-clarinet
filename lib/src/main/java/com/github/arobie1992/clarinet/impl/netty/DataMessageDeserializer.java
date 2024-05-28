package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;

import java.io.IOException;
import java.util.Base64;

public class DataMessageDeserializer extends JsonDeserializer<DataMessage> {
    @Override
    public DataMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var tree = p.getCodec().readTree(p);
        var connectionIdStr = ((TextNode) tree.get("connectionId")).textValue();
        var seqNo = ((IntNode) tree.get("sequenceNumber")).longValue();
        var messageId = new MessageId(ConnectionId.fromString(connectionIdStr), seqNo);
        var data = decodeBytes(tree, "data");
        var message = new DataMessage(messageId, data);
        var senderSignature = decodeBytes(tree, "senderSignature");
        if (senderSignature != null) {
            message.setSenderSignature(senderSignature);
        }
        var witnessSignature = decodeBytes(tree, "witnessSignature");
        if (witnessSignature != null) {
            message.setWitnessSignature(witnessSignature);
        }
        return message;
    }

    private byte[] decodeBytes(TreeNode tree, String fieldName) {
        var node = tree.get(fieldName);
        if(node == null) {
            return null;
        }
        var str = ((TextNode) node).textValue();
        return Base64.getDecoder().decode(str);
    }
}
