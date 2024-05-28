package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

// test serializer and deserializer together to make sure they work
class DataMessageSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DataMessage dataMessage = new DataMessage(new MessageId(ConnectionId.random(), 0), new byte[]{9,7,45});

    DataMessageSerializationTest() {
        var module = new SimpleModule();
        module.addSerializer(DataMessage.class, new DataMessageSerializer());
        module.addDeserializer(DataMessage.class, new DataMessageDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void test() throws IOException {
        var ser = mapper.writeValueAsBytes(dataMessage);
        var read = mapper.readValue(ser, DataMessage.class);
        verifyMatches(dataMessage, read);
    }

    @Test
    void testWithSignatures() throws IOException {
        dataMessage.setSenderSignature(new byte[]{55, 34, 90});
        dataMessage.setWitnessSignature(new byte[]{99, 2, 0});
        var ser = mapper.writeValueAsBytes(dataMessage);
        var read = mapper.readValue(ser, DataMessage.class);
        verifyMatches(dataMessage, read);
    }

    private void verifyMatches(DataMessage expected, DataMessage actual) {
        assertEquals(expected.messageId(), actual.messageId());
        assertArrayEquals(expected.data(), actual.data());
        assertArrayEquals(expected.senderSignature().orElse(null), actual.senderSignature().orElse(null));
        assertArrayEquals(expected.witnessSignature().orElse(null), actual.witnessSignature().orElse(null));
    }

}