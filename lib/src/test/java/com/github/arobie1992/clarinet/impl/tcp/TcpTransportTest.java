package com.github.arobie1992.clarinet.impl.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import lombok.Lombok;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TcpTransportTest {

    private record Message(String contents) {}

    private TcpTransport transport;
    private final Object message = new Message("message");
    private final Object response = new Message("response");
    private final ServerSocket serverSocket = new ServerSocket(0);
    private final Address address = new UriAddress(new URI("tcp://localhost:" + serverSocket.getLocalPort()));
    private final ObjectMapper objectMapper = new ObjectMapper();

    TcpTransportTest() throws URISyntaxException, IOException {
    }

    @BeforeEach
    void setUp() {
        transport = new TcpTransport();
    }

    @Test
    void testExchangeNotTcpAddress() throws URISyntaxException {
        var address = new UriAddress(new URI("udp://localhost"));
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> transport.exchange(address, message, Message.class, TransportUtils.defaultOptions())
        );
        assertEquals("Address is not a tcp address: " + address, ex.getMessage());
    }

    @Test
    void testExchangeIOException() throws URISyntaxException {
        // hopefully nothing is running on this port
        var address = new UriAddress(new URI("tcp://localhost:9999"));
        assertThrows(UncheckedIOException.class, () -> transport.exchange(address, message, Message.class, TransportUtils.defaultOptions()));
    }

    @Test
    void testExchange() throws Throwable {
        var t = AsyncAssert.started(() -> {
            try(var sock = serverSocket.accept()) {
                var in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                var message = objectMapper.readValue(in.readLine(), Message.class);
                assertEquals(this.message, message);
                var out = sock.getOutputStream();
                out.write(objectMapper.writeValueAsBytes(response));
            }
        });
        var response = transport.exchange(address, message, Message.class, TransportUtils.defaultOptions());
        assertEquals(this.response, response);
        t.join();
    }

    // TODO figure out how to test send timeouts
    @Disabled
    @Test
    void testExchangeDefaultSendTimeout() throws URISyntaxException {
        var start = LocalDateTime.now();
        var address = new UriAddress(new URI("tcp://localhost:9999"));
        assertThrows(UncheckedIOException.class, () -> transport.exchange(address, message, Message.class, TransportUtils.defaultOptions()));
        var duration = Duration.between(start, LocalDateTime.now());
        assertTrue(Duration.ofSeconds(9).compareTo(duration) < 0);
        assertTrue(Duration.ofSeconds(11).compareTo(duration) > 0);
    }

    // TODO figure out how to test send timeouts
    @Disabled
    @Test
    void testExchangeSendTimeout() {
        fail("implemented testExchangeSendTimeout");
    }

    @Test
    void testExchangeDefaultReceiveTimeout() throws Throwable {
        var t = AsyncAssert.started(() -> {
            try(var sock = serverSocket.accept()) {
                var in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                var message = objectMapper.readValue(in.readLine(), Message.class);
                assertEquals(this.message, message);
                Thread.sleep(10000);
            }
        });
        var start = LocalDateTime.now();
        assertThrows(UncheckedIOException.class, () -> transport.exchange(address, message, Message.class, TransportUtils.defaultOptions()));
        var duration = Duration.between(start, LocalDateTime.now());
        assertTrue(Duration.ofSeconds(9).compareTo(duration) < 0);
        assertTrue(Duration.ofSeconds(11).compareTo(duration) > 0);
        t.join();
    }

    @Test
    void testExchangeReceiveTimeout() throws Throwable {
        var t = AsyncAssert.started(() -> {
            try(var sock = serverSocket.accept()) {
                var in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                var message = objectMapper.readValue(in.readLine(), Message.class);
                assertEquals(this.message, message);
                Thread.sleep(5000);
            }
        });
        var start = LocalDateTime.now();
        var options = new TransportOptions(Optional.empty(), Optional.of(Duration.ofSeconds(5)));
        assertThrows(UncheckedIOException.class, () -> transport.exchange(address, message, Message.class, options));
        var duration = Duration.between(start, LocalDateTime.now());
        assertTrue(Duration.ofSeconds(4).compareTo(duration) < 0);
        assertTrue(Duration.ofSeconds(6).compareTo(duration) > 0);
        t.join();
    }

    /*
     Don't have a test for nonephemeral port because checking for unused ports is error-prone and this exercises the
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
    void testAddThrowsIOException() {
        assertThrows(UncheckedIOException.class, () -> transport.add(new TestAddress(new IOException())));
    }

    @Test
    void testAddThrowsURISyntaxException() {
        assertThrows(
                UncheckedURISyntaxException.class,
                () -> transport.add(new TestAddress(new URISyntaxException("http://localhost", "test ex")))
        );
    }

    @Test
    void testRemoveSocketIsClosed() throws NoSuchFieldException, IllegalAccessException {
        var socketMock = mock(ServerSocket.class);
        insertSocket(address, socketMock);
        when(socketMock.isClosed()).thenReturn(true);
        var removed = transport.remove(address);
        assertTrue(removed.isPresent());
        assertEquals(address, removed.get());
    }

    @Test
    void testRemoveSocketThrowsIOException() throws NoSuchFieldException, IllegalAccessException, IOException {
        var socketMock = mock(ServerSocket.class);
        insertSocket(address, socketMock);
        doThrow(IOException.class).when(socketMock).close();
        assertThrows(UncheckedIOException.class, () -> transport.remove(address));
        // have to reset the mock so it doesn't cause the exception to get thrown in the @AfterEach teardown method
        reset(socketMock);
    }

    private void insertSocket(Address address, ServerSocket socket) throws NoSuchFieldException, IllegalAccessException {
        var field = transport.getClass().getDeclaredField("serverSockets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var socks = (Map<Address, ServerSocket>) field.get(transport);
        socks.put(address, socket);
    }

    @AfterEach
    void tearDown() {
        transport.addresses().forEach(transport::remove);
    }

}