package com.github.arobie1992.clarinet.message;

import java.util.Arrays;
import java.util.Objects;

public record MessageSummary(MessageId messageId, byte[] hash, String hashAlgorithm, byte[] witnessSignature) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageSummary that = (MessageSummary) o;
        return Objects.deepEquals(hash, that.hash) && Objects.equals(messageId, that.messageId) && Objects.equals(hashAlgorithm, that.hashAlgorithm) && Objects.deepEquals(witnessSignature, that.witnessSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, Arrays.hashCode(hash), hashAlgorithm, Arrays.hashCode(witnessSignature));
    }
}
