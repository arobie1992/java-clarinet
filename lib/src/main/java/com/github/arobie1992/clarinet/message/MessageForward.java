package com.github.arobie1992.clarinet.message;

import java.util.Arrays;
import java.util.Objects;

public record MessageForward(MessageSummary summary, byte[] signature) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageForward that = (MessageForward) o;
        return Objects.deepEquals(signature, that.signature) && Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, Arrays.hashCode(signature));
    }
}
