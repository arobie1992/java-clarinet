package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arobie1992.clarinet.transport.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws IOException {
        var buff = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(buff);
        list.add(objectMapper.readValue(buff, Message.class));
    }
}
