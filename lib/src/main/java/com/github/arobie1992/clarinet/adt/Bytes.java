package com.github.arobie1992.clarinet.adt;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable byte array that implements equality based on the contents.
 */
public class Bytes {
    private final byte[] bytes;

    private Bytes(byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public static Bytes of(byte[] bytes) {
        return new Bytes(bytes);
    }

    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bytes bytes1 = (Bytes) o;
        return Objects.deepEquals(bytes, bytes1.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return Arrays.toString(bytes);
    }
}
