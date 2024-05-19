package com.github.arobie1992.clarinet.transport;

import com.github.arobie1992.clarinet.peer.Address;

import java.util.Collection;
import java.util.Optional;

public interface Transport {
    Address add(Address address);
    Optional<Address> remove(Address address);
    Collection<Address> addresses();
    <T> T exchange(Address address, Object message, Class<T> responseType, TransportOptions options);
}
