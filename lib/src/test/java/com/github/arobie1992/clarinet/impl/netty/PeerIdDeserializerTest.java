package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PeerIdDeserializerTest {

    @Test
    void deserialize() throws IOException {
        var expected = PeerUtils.senderId();
        var deserializer = new PeerIdDeserializer();
        var parser = mock(JsonParser.class);
        when(parser.getText()).thenReturn(expected.asString());
        var ctx = mock(DeserializationContext.class);
        assertEquals(expected, deserializer.deserialize(parser, ctx));
    }

}