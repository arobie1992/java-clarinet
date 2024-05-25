package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.Handler;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.Collection;
import java.util.Optional;

class TransportProxy implements Transport {
    private final Transport transport;

    TransportProxy(Transport transport) {
        this.transport = transport;
    }

    @Override
    public Address add(Address address) {
        return this.transport.add(address);
    }

    @Override
    public Optional<Address> remove(Address address) {
        return this.transport.remove(address);
    }

    @Override
    public Collection<Address> addresses() {
        return transport.addresses();
    }

    @Override
    public void add(String endpoint, Handler<?, ?> handler) {
        if(Endpoints.isEndpoint(endpoint)) {
            throw new IllegalArgumentException("Please use the node-level operations for altering protocol-required endpoint: " + endpoint);
        }
        transport.add(endpoint, handler);
    }

    void addInternal(String endpoint, Handler<?, ?> handler) {
        transport.add(endpoint, handler);
    }

    @Override
    public Optional<Handler<?, ?>> remove(String endpoint) {
        if(Endpoints.isEndpoint(endpoint)) {
            throw new IllegalArgumentException("Please use the node-level operations for altering protocol-required endpoint: " + endpoint);
        }
        return transport.remove(endpoint);
    }

    @Override
    public Collection<String> endpoints() {
        return transport.endpoints();
    }

    @Override
    public <T> T exchange(Address address, String endpoint, Object message, Class<T> responseType, TransportOptions options) {
        return transport.exchange(address, endpoint, message, responseType, options);
    }

    @Override
    public void send(Address address, String endpoint, Object message, TransportOptions options) {
        transport.send(address, endpoint, message, options);
    }

    @Override
    public void shutdown() {
        transport.shutdown();
    }
}
