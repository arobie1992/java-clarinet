package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.peer.Address;

import java.util.Collection;
import java.util.Optional;

public interface Transport {
    Address add(Address address);
    Optional<Address> remove(Address address);
    Collection<Address> addresses();
    void add(String endpoint, Handler<?, ?> handler);
    Optional<Handler<?, ?>> remove(String endpoint);
    Collection<String> endpoints();
    <T> T exchange(Address address, String endpoint, Object message, Class<T> responseType, TransportOptions options);
    void send(Address address, String endpoint, Object message, TransportOptions options);
    void shutdown();
}
