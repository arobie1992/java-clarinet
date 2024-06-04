package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();
    public MessageDecoder() {
        var module = new SimpleModule();
        module.addDeserializer(PeerId.class, new PeerIdDeserializer());
        module.addDeserializer(Address.class, new AddressDeserializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());
    }
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws IOException {
        var buff = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buff);
        list.add(objectMapper.readValue(buff, Message.class));
    }
}
