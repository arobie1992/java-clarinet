package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

import java.util.Objects;
import java.util.Optional;

public class DataMessage {
    private final MessageId messageId;
    private final Bytes data;
    private Bytes senderSignature;
    private Bytes witnessSignature;

    public DataMessage(MessageId messageId, Bytes data) {
        this.messageId = Objects.requireNonNull(messageId);
        this.data = Objects.requireNonNull(data);
    }

    public SenderParts senderParts() {
        return new SenderParts(messageId, data());
    }

    public WitnessParts witnessParts() {
        return new WitnessParts(messageId, data(), senderSignature().orElse(null));
    }

    public MessageId messageId() {
        return messageId;
    }

    public Bytes data() {
        return data;
    }

    public void setSenderSignature(Bytes signature) {
        this.senderSignature = signature;
    }

    public Optional<Bytes> senderSignature() {
        return Optional.ofNullable(senderSignature);
    }

    public void setWitnessSignature(Bytes signature) {
        this.witnessSignature = signature;
    }

    public Optional<Bytes> witnessSignature() {
        return Optional.ofNullable(witnessSignature);
    }

    public record SenderParts(MessageId messageId, Bytes data) {}
    public record WitnessParts(MessageId messageId, Bytes data, Bytes senderSignature) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataMessage message = (DataMessage) o;
        return Objects.equals(messageId, message.messageId)
                && Objects.equals(data, message.data)
                && Objects.equals(senderSignature, message.senderSignature)
                && Objects.equals(witnessSignature, message.witnessSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, data, senderSignature, witnessSignature);
    }
}
