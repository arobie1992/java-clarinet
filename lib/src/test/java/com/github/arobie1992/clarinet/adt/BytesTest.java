package com.github.arobie1992.clarinet.adt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BytesTest {

    private final byte[] expected = {77};

    private byte[] byteArr;
    private Bytes bytes;

    @BeforeEach
    void setup() {
        byteArr = Arrays.copyOf(expected, expected.length);
        bytes = Bytes.of(byteArr);
    }

    @Test
    void testNullBytes() {
        assertThrows(NullPointerException.class, () -> new Bytes(null));
    }

    @Test
    void testCopyIn() {
        assertArrayEquals(expected, bytes.bytes());
        byteArr[0] = 90;
        assertFalse(Arrays.equals(expected, byteArr));
        assertArrayEquals(expected, bytes.bytes());
    }

    @Test
    void testCopyOut() {
        var retrieved = bytes.bytes();
        retrieved[0] = 32;
        assertFalse(Arrays.equals(expected, retrieved));
        assertArrayEquals(expected, byteArr);
        assertArrayEquals(expected, bytes.bytes());
    }

    @Test
    void testEqualsAndHashCodeReflexive() {
        //noinspection EqualsWithItself
        assertEquals(bytes, bytes);
        assertEquals(bytes.hashCode(), bytes.hashCode());
    }

    @Test
    void testSymmetric() {
        var b = Bytes.of(byteArr);

        assertEquals(bytes, b);
        assertEquals(bytes.hashCode(), b.hashCode());

        assertEquals(b, bytes);
        assertEquals(b.hashCode(), bytes.hashCode());
    }

    @Test
    void testTransitive() {
        var b = Bytes.of(byteArr);
        var c = Bytes.of(byteArr);

        assertEquals(bytes, b);
        assertEquals(bytes.hashCode(), b.hashCode());

        assertEquals(b, c);
        assertEquals(b.hashCode(), c.hashCode());

        assertEquals(bytes, c);
        assertEquals(bytes.hashCode(), c.hashCode());
    }

    @Test
    void testConsistent() {
        var b = Bytes.of(byteArr);

        assertEquals(bytes, b);
        assertEquals(bytes.hashCode(), b.hashCode());
        assertEquals(bytes, b);
        assertEquals(bytes.hashCode(), b.hashCode());
        assertEquals(bytes, b);
        assertEquals(bytes.hashCode(), b.hashCode());
    }

    @SuppressWarnings("SimplifiableAssertion")
    @Test
    void testEqualsNull() {
        //noinspection ConstantValue
        assertFalse(bytes.equals(null));
    }

    @Test
    void testEqualsAndHashCodeDifferentContents() {
        var b = Bytes.of(new byte[]{45});
        assertFalse(Arrays.equals(bytes.bytes(), b.bytes()));
        assertNotEquals(bytes, b);
        assertNotEquals(bytes.hashCode(), b.hashCode());
    }

    @Test
    void testEqualsAndHashCodeDifferentClass() {
        var o = new Object();
        assertNotEquals(bytes, o);
        assertNotEquals(bytes.hashCode(), o.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("[77]", bytes.toString());
    }

}