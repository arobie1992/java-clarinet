package com.github.arobie1992.clarinet.impl.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.time.Duration;
import java.util.*;

public class TcpTransport implements Transport {
    private final Map<Address, ServerSocket> serverSockets = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> T exchange(Address address, Object message, Class<T> responseType, TransportOptions options) {
        try(var socket = new Socket()) {
            send(address, socket, options, message);
            var in = socket.getInputStream();
            socket.setSoTimeout(timeoutMillis(options.receiveTimeout()));
            var bytes = in.readAllBytes();
            return objectMapper.readValue(bytes, responseType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void send(Address address, Socket socket, TransportOptions options, Object message) throws IOException {
        var addrUri = validateAddress(address);
        socket.connect(new InetSocketAddress(addrUri.getHost(), addrUri.getPort()), timeoutMillis(options.sendTimeout()));
        var out = socket.getOutputStream();
        out.write(objectMapper.writeValueAsBytes(message));
        // use newline as the terminator symbol for messages
        out.write('\n');
    }

    @Override
    public Address add(Address address) {
        if(serverSockets.containsKey(address)) {
            return address;
        }
        var uri = validateAddress(address);
        try {
            var sock = new ServerSocket();
            serverSockets.put(address, sock);
            sock.bind(new InetSocketAddress(uri.getHost(), uri.getPort()));
            var localSockAddr = (InetSocketAddress) sock.getLocalSocketAddress();
            var retAddr = address.parseFunction().apply(new URI("tcp://" + localSockAddr.getHostName() + ":" + localSockAddr.getPort()));
            if(!retAddr.equals(address)) {
                serverSockets.put(retAddr, serverSockets.remove(address));
            }
            return retAddr;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        }
    }

    @Override
    public Optional<Address> remove(Address address) {
        var sock = serverSockets.get(address);
        if(sock == null) {
            return Optional.empty();
        }
        if(!sock.isClosed()) {
            try {
                sock.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        serverSockets.remove(address);
        return Optional.of(address);
    }

    @Override
    public Collection<Address> addresses() {
        return List.copyOf(serverSockets.keySet());
    }

    // The entire point of the method is convenience for unwrapping the optional
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static int timeoutMillis(Optional<Duration> timeout) {
        var duration = timeout.orElse(Duration.ofSeconds(10));
        return Math.toIntExact(duration.toMillis());
    }

    private static URI validateAddress(Address address) {
        var uri = address.asURI();
        if(!"tcp".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Address is not a tcp address: " + address);
        }
        return uri;
    }

}
