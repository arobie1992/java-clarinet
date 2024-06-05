package com.github.arobie1992.clarinet.adt;

import java.util.Objects;

public record Some<T>(T value) implements Option<T> {
    public Some {
        Objects.requireNonNull(value, "value must not be null");
    }
}
