package com.github.arobie1992.clarinet.core;

import java.time.Duration;
import java.util.Optional;

public record CloseOptions(Optional<Duration> connectionObtainTimeout) {
    public CloseOptions() {
        this(Optional.empty());
    }
}
