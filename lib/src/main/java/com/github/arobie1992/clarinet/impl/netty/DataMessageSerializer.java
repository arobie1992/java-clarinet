package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.message.DataMessage;

import java.io.IOException;
import java.util.Base64;

public class DataMessageSerializer extends JsonSerializer<DataMessage> {
    @Override
    public void serialize(DataMessage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("connectionId", value.messageId().connectionId().toString());
        gen.writeNumberField("sequenceNumber", value.messageId().sequenceNumber());
        writeBytes("data", value.data(), gen);
        if(value.senderSignature().isPresent()) {
            writeBytes("senderSignature", value.senderSignature().get(), gen);
        }
        if(value.witnessSignature().isPresent()) {
            writeBytes("witnessSignature", value.witnessSignature().get(), gen);
        }
        gen.writeEndObject();
    }

    private void writeBytes(String fieldName, Bytes bytes, JsonGenerator gen) throws IOException {
        var encoded = Base64.getEncoder().encodeToString(bytes.bytes());
        gen.writeStringField(fieldName, encoded);
    }
}
