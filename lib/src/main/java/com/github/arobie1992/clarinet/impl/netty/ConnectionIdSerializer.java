package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.arobie1992.clarinet.core.ConnectionId;

import java.io.IOException;

public class ConnectionIdSerializer extends JsonSerializer<ConnectionId> {
    @Override
    public void serialize(ConnectionId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
