package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionIdDeserializerTest {

    @Test
    void testDeserialize() throws IOException {
        var expected = ConnectionId.fromString(UUID.randomUUID().toString());
        var deserializer = new ConnectionIdDeserializer();
        var parser = mock(JsonParser.class);
        var ctx = mock(DeserializationContext.class);
        when(parser.getText()).thenReturn(expected.toString());
        assertEquals(expected, deserializer.deserialize(parser, ctx));
    }

}