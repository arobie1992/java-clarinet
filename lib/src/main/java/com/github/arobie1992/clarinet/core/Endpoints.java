package com.github.arobie1992.clarinet.core;

public enum Endpoints {
    CONNECT,
    WITNESS,
    WITNESS_NOTIFICATION,
    MESSAGE,
    REQUEST_PEERS,
    REQUEST_KEYS,
    QUERY,
    CLOSE;

    static boolean isEndpoint(String endpoint) {
        for (Endpoints e : Endpoints.values()) {
            if (e.name().equals(endpoint)) {
                return true;
            }
        }
        return false;
    }
}
