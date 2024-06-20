package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

import java.util.Objects;

public record MessageForward(MessageSummary summary, Bytes signature) {
    public MessageForward {
        Objects.requireNonNull(summary);
        Objects.requireNonNull(signature);
    }
}
