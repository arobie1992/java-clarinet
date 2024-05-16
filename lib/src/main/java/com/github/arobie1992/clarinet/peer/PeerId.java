package com.github.arobie1992.clarinet.peer;

import java.util.function.Function;

public interface PeerId {
    String asString();
    Function<String, PeerId> parseFunction();
}
