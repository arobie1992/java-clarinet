package com.github.arobie1992.clarinet.impl.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arobie1992.clarinet.peer.ReadOnlyPeer;
import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.TestPeerId;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class TcpTransportTest {

    private record TestMessage(String value) {
        TestMessage() {
            this("test");
        }
    }

    private final TcpTransport transport = new TcpTransport();
    private final ServerSocket serverSocket = new ServerSocket(0);
    private final ReadOnlyPeer testPeer = new ReadOnlyPeer(new TestPeerId(), "tcp://0.0.0.0:" + serverSocket.getLocalPort());
    private final TestMessage testMessage = new TestMessage();
    private final ObjectMapper objectMapper = new ObjectMapper();

    TcpTransportTest() throws IOException {}

    @Test
    void testSendInvalidUri() {
        var peer = new ReadOnlyPeer(new TestPeerId(), ":0.0.0.0");
        assertThrows(UncheckedURISyntaxException.class, () -> transport.send(peer, TransportUtils.defaultOptions(), new Object()));
    }

    @Test
    void testSendIOException() throws IOException {
        var omMock = mock(ObjectMapper.class);
        var transport = new TcpTransport(omMock);
        doThrow(JsonProcessingException.class).when(omMock).writeValueAsBytes(testMessage);
        assertThrows(UncheckedIOException.class, () -> transport.send(testPeer, TransportUtils.defaultOptions(), testMessage));
    }

    @Test
    void testSend() throws IOException {
        transport.send(testPeer, TransportUtils.defaultOptions(), testMessage);
        try(var sock = serverSocket.accept()) {
            try(var in = sock.getInputStream()) {
                var bytes = in.readAllBytes();
                var readMessage = objectMapper.readValue(bytes, TestMessage.class);
                assertEquals(testMessage, readMessage);
            }
        }
    }

    @Test
    void testExchangeInvalidUri() {
        var peer = new ReadOnlyPeer(new TestPeerId(), ":0.0.0.0");
        assertThrows(
                UncheckedURISyntaxException.class,
                () -> transport.exchange(peer, TransportUtils.defaultOptions(), testMessage, TestMessage.class)
        );
    }

    @Test
    void testExchangeIOException() throws IOException {
        var omMock = mock(ObjectMapper.class);
        var transport = new TcpTransport(omMock);
        doThrow(JsonProcessingException.class).when(omMock).writeValueAsBytes(testMessage);
        assertThrows(
                UncheckedIOException.class,
                () -> transport.exchange(testPeer, TransportUtils.defaultOptions(), testMessage, TestMessage.class)
        );
    }

    @Test
    void testExchange() throws InterruptedException {
        var expectedResponse = new TestMessage("resp");
        var t = AsyncAssert.started(() -> {
            try(
                    var sock = serverSocket.accept();
                    var in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    var out = sock.getOutputStream()
            ) {
                var bytes = in.readLine();
                var readMessage = objectMapper.readValue(bytes, TestMessage.class);
                assertEquals(testMessage, readMessage);
                out.write(objectMapper.writeValueAsBytes(expectedResponse));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        var response = transport.exchange(testPeer, TransportUtils.defaultOptions(), testMessage, TestMessage.class);
        assertEquals(expectedResponse, response);
        t.join();
    }

}