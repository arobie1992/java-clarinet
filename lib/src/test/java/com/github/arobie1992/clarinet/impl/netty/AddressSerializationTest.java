package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.transport.UncheckedURISyntaxException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressSerializationTest {

    private final Address address = AddressUtils.defaultAddress();
    private final ObjectMapper objectMapper = new ObjectMapper();

    AddressSerializationTest() {
        var module = new SimpleModule();
        module.addSerializer(Address.class, new AddressSerializer());
        module.addDeserializer(Address.class, new AddressDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    void testSerde() throws IOException {
        var ser = objectMapper.writeValueAsBytes(address);
        var read = objectMapper.readValue(ser, Address.class);
        assertEquals(address, read);
    }

    @Test
    void testDeserializeThrowsUriException() {
        assertThrows(UncheckedURISyntaxException.class, () -> objectMapper.readValue("\"://missingscheme\"", Address.class));
    }

}