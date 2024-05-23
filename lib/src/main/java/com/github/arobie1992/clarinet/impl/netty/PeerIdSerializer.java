package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.io.IOException;

class PeerIdSerializer extends JsonSerializer<PeerId> {
    @Override
    public void serialize(PeerId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.asString());
    }
}
