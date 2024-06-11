package com.github.arobie1992.clarinet.message;

import java.util.Arrays;
import java.util.Objects;

/**
 * The data this node has for the queried message.
 * <p>
 * If the node does not have a record of the message, it must return null for all three fields. If the node does have a
 * record of the message, then it must ensure it uses the appropriate fields for itself and the given requestor. If this
 * node was the sender in the connection or the querying node was the sender, this node must use
 * {@link DataMessage#senderParts()} for appropriate operations. Otherwise, it must use
 * {@link DataMessage#witnessParts()}.
 * @param hash The hash of the appropriate parts for the queried message.
 * @param signature The signature of {@code hash}.
 * @param hashAlgorithm The hashing algorithm this node used to generate {@code hash}.
 */
public record QueryResponse(byte[] hash, byte[] signature, String hashAlgorithm) {
    public QueryResponse(byte[] hash, byte[] signature, String hashAlgorithm) {
        this.hash = hash == null ? null : Arrays.copyOf(hash, hash.length);
        this.signature = hash == null ? null : Arrays.copyOf(signature, signature.length);
        this.hashAlgorithm = hashAlgorithm;
    }
    public byte[] hash() {
        return hash == null ? null : Arrays.copyOf(hash, hash.length);
    }
    public byte[] signature() {
        return hash == null ? null : Arrays.copyOf(signature, signature.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResponse that = (QueryResponse) o;
        return Objects.deepEquals(hash, that.hash)
                && Objects.deepEquals(signature, that.signature)
                && Objects.equals(hashAlgorithm, that.hashAlgorithm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(hash), Arrays.hashCode(signature), hashAlgorithm);
    }
}
