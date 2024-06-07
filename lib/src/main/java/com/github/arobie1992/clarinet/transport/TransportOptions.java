package com.github.arobie1992.clarinet.transport;

import java.time.Duration;
import java.util.Optional;

public record TransportOptions(Optional<Duration> sendTimeout, Optional<Duration> receiveTimeout) {
    public TransportOptions() {
        this(Optional.empty(), Optional.empty());
    }
}
