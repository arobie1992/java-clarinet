package com.github.arobie1992.clarinet.testutils;

import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import java.util.Arrays;
import java.util.Optional;

public class ArgumentMatcherUtils {
    private ArgumentMatcherUtils() {}

    private record OptionalByteArrayEqualing(Optional<byte[]> expected) implements ArgumentMatcher<Optional<byte[]>> {
        @Override
        public boolean matches(Optional<byte[]> argument) {
            return expected.map(value -> argument.filter(bytes -> Arrays.equals(value, bytes)).isPresent()).orElseGet(argument::isEmpty);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // it's literally the point
    public static Optional<byte[]> optionalByteArrayEq(Optional<byte[]> expected) {
        return ArgumentMatchers.argThat(new OptionalByteArrayEqualing(expected));
    }

    private record ByteArrayEqualing(byte[] expected) implements ArgumentMatcher<byte[]> {
        @Override
        public boolean matches(byte[] argument) {
            return Arrays.equals(expected, argument);
        }
    }

    public static byte[] byteArrayEq(byte[] expected) {
        return ArgumentMatchers.argThat(new ByteArrayEqualing(expected));
    }

}
