package com.github.arobie1992.clarinet.message;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class DataMessage {
    private final MessageId messageId;
    private final byte[] data;
    private byte[] senderSignature;
    private byte[] witnessSignature;

    public DataMessage(MessageId messageId, byte[] data) {
        this.messageId = messageId;
        this.data = Arrays.copyOf(Objects.requireNonNull(data), data.length);
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

    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public void setSenderSignature(byte[] signature) {
        this.senderSignature = Arrays.copyOf(signature, signature.length);
    }

    public Optional<byte[]> senderSignature() {
        if(senderSignature == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.copyOf(senderSignature, senderSignature.length));
    }

    public void setWitnessSignature(byte[] signature) {
        this.witnessSignature = Arrays.copyOf(signature, signature.length);
    }

    public Optional<byte[]> witnessSignature() {
        if(witnessSignature == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.copyOf(witnessSignature, witnessSignature.length));
    }

    public record SenderParts(MessageId messageId, byte[] data) {}
    public record WitnessParts(MessageId messageId, byte[] data, byte[] senderSignature) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataMessage message = (DataMessage) o;
        return Objects.equals(messageId, message.messageId) && Objects.deepEquals(data, message.data) && Objects.deepEquals(senderSignature, message.senderSignature) && Objects.deepEquals(witnessSignature, message.witnessSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, Arrays.hashCode(data), Arrays.hashCode(senderSignature), Arrays.hashCode(witnessSignature));
    }
}
