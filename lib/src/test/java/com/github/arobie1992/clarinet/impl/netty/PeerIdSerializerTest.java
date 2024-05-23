package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PeerIdSerializerTest {

    @Test
    void testSerialize() throws IOException {
        var peerId = PeerUtils.senderId();
        var serializer = new PeerIdSerializer();
        var gen = mock(JsonGenerator.class);
        var serializers = mock(SerializerProvider.class);
        serializer.serialize(peerId, gen, serializers);
        verify(gen).writeString(peerId.asString());
    }

}