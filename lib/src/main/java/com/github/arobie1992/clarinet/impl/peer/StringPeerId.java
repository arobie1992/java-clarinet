package com.github.arobie1992.clarinet.impl.peer;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.function.Function;

public record StringPeerId(String value) implements PeerId {
    @Override
    public String asString() {
        return value;
    }

    @Override
    public Function<String, PeerId> parseFunction() {
        return StringPeerId::new;
    }
}
