package com.github.arobie1992.clarinet.message;

import java.util.Arrays;

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
}
