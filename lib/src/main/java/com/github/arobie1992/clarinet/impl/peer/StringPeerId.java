package com.github.arobie1992.clarinet.impl.peer;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.function.Function;

public record StringPeerId(String value) implements PeerId {
    @Override
    public String asString() {
        return value;
    }

    // TODO Probably going to come back to these parse functions to see if this setup is actually useful
    @Override
    public Function<String, PeerId> parseFunction() {
        return StringPeerId::new;
    }
}
