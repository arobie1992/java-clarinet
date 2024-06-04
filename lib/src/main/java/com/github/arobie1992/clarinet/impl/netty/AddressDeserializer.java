package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.UncheckedURISyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AddressDeserializer extends JsonDeserializer<Address> {
    @Override
    public Address deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            var uri = new URI(p.getText());
            return new UriAddress(uri);
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        }
    }
}
