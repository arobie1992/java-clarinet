package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.io.IOException;

class PeerIdDeserializer extends JsonDeserializer<PeerId> {
    @Override
    public PeerId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new StringPeerId(p.getText());
    }
}
