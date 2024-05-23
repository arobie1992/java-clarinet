package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.arobie1992.clarinet.core.ConnectionId;

import java.io.IOException;

public class ConnectionIdDeserializer extends JsonDeserializer<ConnectionId> {
    @Override
    public ConnectionId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return ConnectionId.fromString(p.getText());
    }
}
