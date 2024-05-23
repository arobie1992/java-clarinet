package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConnectionIdSerializerTest {

    @Test
    void testSerialize() throws IOException {
        var connectionId = ConnectionId.fromString(UUID.randomUUID().toString());
        var serializer = new ConnectionIdSerializer();
        var gen = mock(JsonGenerator.class);
        var serializers = mock(SerializerProvider.class);
        serializer.serialize(connectionId, gen, serializers);
        verify(gen).writeString(connectionId.toString());
    }

}