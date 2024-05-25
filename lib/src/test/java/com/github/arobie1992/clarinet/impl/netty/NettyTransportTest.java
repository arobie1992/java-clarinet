package com.github.arobie1992.clarinet.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.testutils.ReflectionTestUtils;
import com.github.arobie1992.clarinet.testutils.ThreadUtils;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import com.github.arobie1992.clarinet.transport.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import lombok.Lombok;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NettyTransportTest {

    private record Message(String contents) {}
    private static class TestHandler implements Handler<Message, Object> {
        Function<Message, Object> delegate;
        private Address remoteAddress;
        private Message receivedMessage;

        @Override
        public Object handle(Address remoteAddress, Message message) {
            this.remoteAddress = remoteAddress;
            receivedMessage = message;
            return delegate.apply(message);
        }
        @Override
        public Class<Message> inputType() {
            return Message.class;
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NettyTransport transport = new NettyTransport(TransportUtils.defaultOptions());
    private final Message message = new Message("message");
    private final Message response = new Message("response");
    private final String exchangeEndpoint = "exchange";
    private final TestHandler exchangeHandler = new TestHandler();
    private final String sendEndpoint = "send";
    private final TestHandler sendHandler = new TestHandler();

    private Address address;
    private CountDownLatch sendLatch;

    NettyTransportTest() {
        objectMapper.registerModule(new Jdk8Module());
    }

    @BeforeEach
    void setUp() throws URISyntaxException {
        address = transport.add(new UriAddress(new URI("tcp://localhost:0")));

        exchangeHandler.remoteAddress = null;
        exchangeHandler.receivedMessage = null;
        exchangeHandler.delegate = ignoredMessage -> response;
        transport.add(exchangeEndpoint, exchangeHandler);

        sendHandler.remoteAddress = null;
        sendHandler.receivedMessage = null;
        sendLatch = new CountDownLatch(1);
        sendHandler.delegate = ignoredMessage -> {
            sendLatch.countDown();
            return null;
        };
        transport.add(sendEndpoint, sendHandler);
    }

    @Test
    void testAddNonTcpAddress() throws URISyntaxException {
        var address = new UriAddress(new URI("http://localhost:0"));
        var ex = assertThrows(IllegalArgumentException.class, () -> transport.add(address));
        assertEquals("Address is not a tcp address: " + address, ex.getMessage());
    }

    @Test
    void testExchange() {
        // just send to itself because there's no reason it shouldn't work
        var resp = transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions());
        assertEquals(message, exchangeHandler.receivedMessage);
        assertEquals(response, resp);
    }

    @Test
    void testSend() throws InterruptedException {
        // just send to itself because there's no reason it shouldn't work
        transport.send(address, sendEndpoint, message, TransportUtils.defaultOptions());
        // need to wait for the send to complete so that the cleanup doesn't remove the handlers before the dispatcher starts
        sendLatch.await();
        assertEquals(message, sendHandler.receivedMessage);
    }

    @Test
    void testExchangeNotTcpAddress() throws URISyntaxException {
        var address = new UriAddress(new URI("udp://localhost"));
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions())
        );
        assertEquals("Address is not a tcp address: " + address, ex.getMessage());
    }

    @Test
    void testSendNotTcpAddress() throws URISyntaxException {
        var address = new UriAddress(new URI("udp://localhost"));
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> transport.send(address, sendEndpoint, message, TransportUtils.defaultOptions())
        );
        assertEquals("Address is not a tcp address: " + address, ex.getMessage());
    }

    @Test
    void testExchangeIOException() throws URISyntaxException {
        // hopefully nothing is running on this port
        var address = new UriAddress(new URI("tcp://localhost:9999"));
        assertThrows(
                UncheckedIOException.class,
                () -> transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions())
        );
    }

    @Test
    void testSendIOException() throws URISyntaxException {
        // hopefully nothing is running on this port
        var address = new UriAddress(new URI("tcp://localhost:9999"));
        assertThrows(UncheckedIOException.class, () -> transport.send(address, sendEndpoint, message, TransportUtils.defaultOptions()));
    }

    @Test
    void testExchangeMismatchedReturnType() throws Throwable {
        var mismatchedResp = List.of("Test error");
        exchangeHandler.delegate = message -> mismatchedResp;
        var ex = assertThrows(
                MismatchedResponseTypeException.class,
                () -> transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions())
        );
        assertEquals(objectMapper.writeValueAsString(mismatchedResp), ex.response());
        assertEquals(Message.class, ex.responseType());
        assertEquals(message, exchangeHandler.receivedMessage);
    }

    @Test
    void testExchangeHandlerGetsAddress() throws IOException, URISyntaxException {
        var addrUri = address.asURI();
        try(var sock = new Socket()) {
            sock.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()), 1000);
            var out = sock.getOutputStream();
            out.write(objectMapper.writeValueAsBytes(new com.github.arobie1992.clarinet.transport.Message(exchangeEndpoint, message)));
            sock.setSoTimeout(1000);
            var bytes = sock.getInputStream().readAllBytes();
            assertEquals(response, objectMapper.readValue(bytes, Message.class));
            var expectedAddress = new UriAddress(new URI("tcp://" + sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort()));
            assertEquals(expectedAddress, exchangeHandler.remoteAddress);
        }
    }

    @Test
    void testSendHandlerGetsAddress() throws IOException, URISyntaxException, InterruptedException {
        var addrUri = address.asURI();
        try(var sock = new Socket()) {
            sock.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()), 1000);
            var out = sock.getOutputStream();
            out.write(objectMapper.writeValueAsBytes(new com.github.arobie1992.clarinet.transport.Message(sendEndpoint, message)));
            var expectedAddress = new UriAddress(new URI("tcp://" + sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort()));
            // need to wait for the send to complete so that the cleanup doesn't remove the handlers before the dispatcher starts
            sendLatch.await();
            assertEquals(expectedAddress, sendHandler.remoteAddress);
        }
    }

    // TODO figure out how to test send timeouts
    @Disabled
    @Test
    void testExchangeDefaultSendTimeout() {
        fail("implemented testExchangeDefaultSendTimeout");
    }

    // TODO figure out how to test send timeouts
    @Disabled
    @Test
    void testExchangeSendTimeout() {
        fail("implemented testExchangeSendTimeout");
    }

    @Test
    void testExchangeDefaultReceiveTimeout() {
        exchangeHandler.delegate = message -> {
            ThreadUtils.sleepUnchecked(10000);
            return Optional.empty();
        };
        var start = LocalDateTime.now();
        assertThrows(
                UncheckedIOException.class,
                () -> transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions())
        );
        var duration = Duration.between(start, LocalDateTime.now());
        assertTrue(Duration.ofSeconds(9).compareTo(duration) < 0);
        assertTrue(Duration.ofSeconds(11).compareTo(duration) > 0);
        assertEquals(message, exchangeHandler.receivedMessage);
    }

    @Test
    void testExchangeReceiveTimeout() {
        exchangeHandler.delegate = message -> {
            ThreadUtils.sleepUnchecked(5000);
            return Optional.empty();
        };
        var start = LocalDateTime.now();
        var options = new TransportOptions(Optional.empty(), Optional.of(Duration.ofSeconds(5)));
        assertThrows(UncheckedIOException.class, () -> transport.exchange(address, exchangeEndpoint, message, Message.class, options));
        var duration = Duration.between(start, LocalDateTime.now());
        assertTrue(Duration.ofSeconds(4).compareTo(duration) < 0);
        assertTrue(Duration.ofSeconds(6).compareTo(duration) > 0);
        assertEquals(message, exchangeHandler.receivedMessage);
    }

    @Test
    void testExchangeHandlerThrowsError() {
        var exMessage = "This is a test exception";
        exchangeHandler.delegate = message -> {
            throw new RuntimeException(exMessage);
        };
        var ex = assertThrows(
                ExchangeErrorException.class,
                () -> transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions())
        );
        assertEquals(exMessage, ex.error());
    }

    @Test
    void testExchangeNoEndpoint() {
        var ex = assertThrows(
                ExchangeErrorException.class,
                () -> transport.exchange(address, "notThere", message, Message.class, TransportUtils.defaultOptions())
        );
        assertEquals("No such endpoint: notThere", ex.error());
    }

    @Test
    void testExchangeHandlerThrowsNoMessage() {
        exchangeHandler.delegate = message -> {
            throw new RuntimeException();
        };
        var ex = assertThrows(
                ExchangeErrorException.class,
                () -> transport.exchange(address, exchangeEndpoint, message, Message.class, TransportUtils.defaultOptions())
        );
        assertEquals("Unspecified error", ex.error());
    }

    @Test
    void testHandlerDefaultReceiveTimeout() throws IOException {
        try(var sock = new Socket()) {
            var addrUri = address.asURI();
            sock.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()));
            ThreadUtils.sleepUnchecked(10000);
            var bytes = sock.getInputStream().readAllBytes();
            var failureResp = objectMapper.readValue(bytes, ErrorResponse.class);
            assertEquals("Read timeout", failureResp.error());
        }
    }

    @Test
    void testHandlerReceiveTimeout() throws IOException, URISyntaxException {
        var options = new TransportOptions(Optional.empty(), Optional.of(Duration.ofSeconds(5)));
        try(var transport = new NettyTransport(options)) {
            var address = transport.add(new UriAddress(new URI("tcp://localhost:0")));
            exchangeHandler.delegate = ignoredMessage -> response;
            transport.add(exchangeEndpoint, exchangeHandler);
            try(var sock = new Socket()) {
                var addrUri = address.asURI();
                sock.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()));
                ThreadUtils.sleepUnchecked(5000);
                var bytes = sock.getInputStream().readAllBytes();
                var failureResp = objectMapper.readValue(bytes, ErrorResponse.class);
                assertEquals("Read timeout", failureResp.error());
            }
        }
    }

    /*
     Don't have a test for non-ephemeral port because checking for unused ports is error-prone and this exercises the
     vast majority of the important behavior.
     */
    @Test
    void testAddEphemeralAndRemove() throws URISyntaxException {
        var address = new UriAddress(new URI("tcp://localhost:0"));
        var addedAddr = transport.add(address);
        var secondAddedAddr = transport.add(addedAddr);
        assertEquals(addedAddr, secondAddedAddr);

        assertTrue(transport.remove(address).isEmpty());

        var removed = transport.remove(addedAddr);
        assertTrue(removed.isPresent());
        assertEquals(addedAddr, removed.get());

        assertTrue(transport.remove(address).isEmpty());
    }

    private record TestAddress(Exception e) implements Address {
        @Override
        public URI asURI() {
            try {
                return new URI("tcp://localhost:0");
            } catch (URISyntaxException e) {
                throw new UncheckedURISyntaxException(e);
            }
        }

        @Override
        public Function<URI, Address> parseFunction() {
            return uri -> {
                throw Lombok.sneakyThrow(e);
            };
        }
    }

    @Test
    void testAddThrowsURISyntaxException() {
        assertThrows(
                UncheckedURISyntaxException.class,
                () -> transport.add(new TestAddress(new URISyntaxException("http://localhost", "test ex")))
        );
    }

    @Test
    void testRemoveUnsuccessful() throws NoSuchFieldException, IllegalAccessException, URISyntaxException {
        @SuppressWarnings("unchecked") // Cast is to get around type erasure; should be fine unless we change backing impl.
        var channels = (Map<Address, ChannelFuture>) ReflectionTestUtils.getFieldValue(transport, "channels", Map.class);
        var channelFutureMock = mock(ChannelFuture.class);
        // use a different address so it doesn't get screwy with not cleaning up the channels
        var address = new UriAddress(new URI("tcp;//localhost:0"));
        channels.put(address, channelFutureMock);

        var channelMock = mock(Channel.class);
        when(channelFutureMock.channel()).thenReturn(channelMock);

        var closeFutureMock = mock(ChannelFuture.class);
        when(channelMock.close()).thenReturn(closeFutureMock);
        when(closeFutureMock.syncUninterruptibly()).thenReturn(closeFutureMock);
        when(closeFutureMock.isSuccess()).thenReturn(false);
        var ex = assertThrows(IllegalStateException.class, () -> transport.remove(address));
        assertEquals("close failed but syncUninterruptibly did not throw an exception", ex.getMessage());
        assertTrue(channels.containsKey(address));

        when(closeFutureMock.isSuccess()).thenReturn(true);
        var addrOpt = transport.remove(address);
        assertTrue(addrOpt.isPresent());
        assertEquals(address, addrOpt.get());
    }

    @Test
    void testRemoveHandler() {
        var handlerOpt = transport.remove(exchangeEndpoint);
        assertTrue(handlerOpt.isPresent());
        assertSame(exchangeHandler, handlerOpt.get());
        assertTrue(transport.remove(exchangeEndpoint).isEmpty());
    }

    @AfterEach
    void tearDown() {
        transport.addresses().forEach(transport::remove);
        transport.endpoints().forEach(transport::remove);
    }

    // implicitly serves as a test of close
    @AfterAll
    void cleanup() throws NoSuchFieldException, IllegalAccessException {
        transport.shutdown();
        var workerGroup = ReflectionTestUtils.getFieldValue(transport, "workerGroup", EventLoopGroup.class);
        assertTrue(workerGroup.isShutdown());
        var bossGroup = ReflectionTestUtils.getFieldValue(transport, "bossGroup", EventLoopGroup.class);
        assertTrue(bossGroup.isShutdown());
        @SuppressWarnings("unchecked") // Cast is to get around type erasure; should be fine unless we change backing impl.
        var channels = (Map<Address, ChannelFuture>) ReflectionTestUtils.getFieldValue(transport, "channels", Map.class);
        assertTrue(channels.isEmpty());
        @SuppressWarnings("unchecked") // Cast is to get around type erasure; should be fine unless we change backing impl.
        var handlers = (Map<String, Handler<Object, Object>>) ReflectionTestUtils.getFieldValue(transport, "handlers", Map.class);
        assertTrue(handlers.isEmpty());
    }

}