package com.github.arobie1992.clarinet.peer;

import java.util.function.Function;

/**
 * The unique identifier for a node.
 * @implSpec must override {@link Object#equals(Object)} and {@link Object#hashCode()} to ensure correctness.
 */
public interface PeerId {
    String asString();
    Function<String, PeerId> parseFunction();
}
