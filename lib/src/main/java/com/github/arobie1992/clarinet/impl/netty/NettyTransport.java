package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NettyTransport implements Transport, AutoCloseable {

    private final Map<Address, ChannelFuture> channels = new ConcurrentHashMap<>();
    private final Map<String, Handler<Object, Object>> handlers = new ConcurrentHashMap<>();
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final ServerBootstrap serverBootstrap;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NettyTransport(TransportOptions transportOptions) {
        var handlerReceiveTimeout = timeoutMillis(transportOptions.receiveTimeout());
        serverBootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(@SuppressWarnings("NullableProblems") SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(
                                new JsonObjectDecoder(),
                                new MessageDecoder(),
                                // read timeout goes first so HandlerDispatcher's error handler will trigger
                                new ReadTimeoutHandler(handlerReceiveTimeout, TimeUnit.MILLISECONDS),
                                new HandlerDispatcher(handlers)
                        );
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        var module = new SimpleModule();
        module.addSerializer(PeerId.class, new PeerIdSerializer());
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new Jdk8Module());
    }

    private URI validateAddress(Address address) {
        var uri = address.asURI();
        if(!"tcp".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Address is not a tcp address: " + address);
        }
        return uri;
    }

    // The entire point of the method is convenience for unwrapping the optional
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static int timeoutMillis(Optional<Duration> timeout) {
        var duration = timeout.orElse(Duration.ofSeconds(10));
        return Math.toIntExact(duration.toMillis());
    }

    @Override
    public Address add(Address address) {
        if(channels.containsKey(address)) {
            return address;
        }
        var addrUri = validateAddress(address);
        var future = serverBootstrap.bind(addrUri.getHost(), addrUri.getPort());
        channels.put(address, future);
        future.awaitUninterruptibly();
        var localAddr = (InetSocketAddress) future.channel().localAddress();
        Address retAddr;
        try {
            retAddr = address.parseFunction().apply(new URI("tcp://" + localAddr.getHostName() + ":" + localAddr.getPort()));
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        }
        if(!retAddr.equals(address)) {
            channels.put(retAddr, channels.remove(address));
        }
        return retAddr;
    }

    @Override
    public Optional<Address> remove(Address address) {
        var channel = channels.get(address);
        if(channel == null) {
            return Optional.empty();
        }
        var fut = channel.channel().close().syncUninterruptibly();
        /*
         syncUninterruptibly should throw any exceptions that caused it to fail, so if we're at this point
         then it should have closed successfully, but do the check because it can't hurt.
         */
        if(!fut.isSuccess()) {
            throw new IllegalStateException("close failed but syncUninterruptibly did not throw an exception");
        }
        channels.remove(address);
        return Optional.of(address);
    }

    @Override
    public Collection<Address> addresses() {
        return List.copyOf(channels.keySet());
    }

    @Override
    public void add(String endpoint, Handler<?, ?> handler) {
        /*
         We're just concerned with converting it and passing it along. The generics are more to ensure that
         implementations of the handlers follow the rules.
         */
        //noinspection unchecked
        handlers.put(endpoint, (Handler<Object, Object>) handler);
    }

    @Override
    public Optional<Handler<?, ?>> remove(String endpoint) {
        return Optional.ofNullable(handlers.remove(endpoint));
    }

    @Override
    public Collection<String> endpoints() {
        return List.copyOf(handlers.keySet());
    }

    @Override
    public <T> T exchange(Address address, String endpoint, Object message, Class<T> responseType, TransportOptions options) {
        // Just use sockets because we don't really need the fanciness of Netty for the client side yet.
        // See guide here if it becomes an issue: https://netty.io/wiki/user-guide-for-4.x.html
        var addrUri = validateAddress(address);
        try(var sock = new Socket()) {
            sock.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()), timeoutMillis(options.sendTimeout()));
            var out = sock.getOutputStream();
            out.write(objectMapper.writeValueAsBytes(new Message(endpoint, message)));
            sock.setSoTimeout(timeoutMillis(options.receiveTimeout()));
            var bytes = sock.getInputStream().readAllBytes();
            try {
                return objectMapper.readValue(bytes, responseType);
            } catch (JsonProcessingException e) {
                try {
                    var errorResponse = objectMapper.readValue(bytes, ErrorResponse.class);
                    throw new ExchangeErrorException(errorResponse.error());
                } catch (JsonProcessingException e1) {
                    throw new MismatchedResponseTypeException(new String(bytes), responseType);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void shutdown() {
        close();
    }

    @Override
    public void close() {
        /*
         Looks like netty handles shutting down the channels:
         https://stackoverflow.com/questions/19747323/netty-stop-reconnecting-and-shutdown
         */
        workerGroup.shutdownGracefully().syncUninterruptibly();
        bossGroup.shutdownGracefully().syncUninterruptibly();
        channels.clear();
        handlers.clear();
    }
}
