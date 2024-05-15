package com.github.arobie1992.clarinet.testutils;

import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.Optional;

public class TransportUtils {
    private TransportUtils() {}

    public static TransportOptions defaultOptions() {
        return new TransportOptions(Optional.empty(), Optional.empty());
    }
}
