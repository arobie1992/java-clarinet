package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

import java.util.Objects;

public record MessageSummary(MessageId messageId, Bytes hash, String hashAlgorithm, Bytes witnessSignature) {
    public MessageSummary {
        Objects.requireNonNull(messageId);
    }
}
