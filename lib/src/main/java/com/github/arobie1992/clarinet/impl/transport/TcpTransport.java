package com.github.arobie1992.clarinet.impl.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;

public class TcpTransport implements Transport {

    private static final Logger LOG = LoggerFactory.getLogger(TcpTransport.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private int timeoutMillis(Optional<Duration> timeout) {
        var duration = timeout.orElse(Duration.ofSeconds(10));
        return Math.toIntExact(duration.toMillis());
    }

    @Override
    public void send(Peer peer, TransportOptions options, Object message) {
        try(var socket = new Socket()) {
            var addrUri = new URI(peer.address());
            socket.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()), timeoutMillis(options.connectTimeout()));
            try(var out = socket.getOutputStream()) {
                out.write(objectMapper.writeValueAsBytes(message));
            }
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> T exchange(Peer peer, TransportOptions options, Object message, Class<T> responseType) {
        try(var socket = new Socket()) {
            var addrUri = new URI(peer.address());
            socket.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()), timeoutMillis(options.connectTimeout()));
            try(var out = socket.getOutputStream()) {
                out.write(objectMapper.writeValueAsBytes(message));
            }
            socket.setSoTimeout(timeoutMillis(options.readTimeout()));
            try(var in = socket.getInputStream()) {
                var bytes = in.readAllBytes();
                return objectMapper.readValue(bytes, responseType);
            }
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
