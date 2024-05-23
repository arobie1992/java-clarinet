package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.ErrorResponse;
import com.github.arobie1992.clarinet.transport.Handler;
import com.github.arobie1992.clarinet.transport.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

// TODO deal with receive timeout: https://stackoverflow.com/questions/37271523/netty-configure-timeouts-on-tcp-server
class HandlerDispatcher extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(HandlerDispatcher.class);

    private final Map<String, Handler<Object, Object>> handlers;
    private final ObjectMapper objectMapper = new ObjectMapper();

    HandlerDispatcher(Map<String, Handler<Object, Object>> handlers) {
        this.handlers = handlers;
        var module = new SimpleModule();
        module.addDeserializer(PeerId.class, new PeerIdDeserializer());
        module.addDeserializer(ConnectionId.class, new ConnectionIdDeserializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());
    }

    @Override
    public void channelRead(
            // IntelliJ was complaining about these being marked as @NotNull, and it didn't seem worth adding the annotations dependency
            @SuppressWarnings("NullableProblems") ChannelHandlerContext ctx,
            @SuppressWarnings("NullableProblems") Object msg
    ) throws JsonProcessingException, URISyntaxException {
        var message = (Message) msg;
        var handler = handlers.get(message.endpoint());
        if (handler == null) {
            throw new NoSuchEndpointException(message.endpoint());
        }
        // Make sure that the contents is parsed as the type the handler is expecting.
        // This is a pretty awful approach, but it should work for now.
        Object contents = objectMapper.readValue(objectMapper.writeValueAsString(message.contents()), handler.inputType());
        var remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
        var resp = handler.handle(new UriAddress(new URI("tcp://" + remoteAddr.getHostName() + ":" + remoteAddr.getPort())), contents);
        if (resp != null) {
            writeResponse(ctx, resp);
        }
        ctx.close();
    }

    // Only need to handle errors here because it is the last in the pipeline
    // If this changes, will have to revisit
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws JsonProcessingException {
        log.warn("Encountered error while handling incoming", cause);
        var message = cause.getMessage();
        if(message == null && cause instanceof ReadTimeoutException) {
            message = "Read timeout";
        }
        writeResponse(ctx, new ErrorResponse(message == null ? "Unspecified error" : message));
        ctx.close();
    }

    private void writeResponse(ChannelHandlerContext ctx, Object response) throws JsonProcessingException {
        var respBytes = objectMapper.writeValueAsBytes(response);
        var encoded = ctx.alloc().buffer(respBytes.length);
        encoded.writeBytes(respBytes);
        ctx.writeAndFlush(encoded);
    }
}
