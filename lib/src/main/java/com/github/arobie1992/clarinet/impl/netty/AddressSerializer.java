package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.arobie1992.clarinet.peer.Address;

import java.io.IOException;

public class AddressSerializer extends JsonSerializer<Address> {
    @Override
    public void serialize(Address value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.asURI().toString());
    }
}
