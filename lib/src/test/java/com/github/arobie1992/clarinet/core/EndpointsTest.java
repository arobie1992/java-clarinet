package com.github.arobie1992.clarinet.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndpointsTest {

    @ParameterizedTest
    @MethodSource("values")
    void testIsEndpoint(String value, boolean expected) {
        assertEquals(expected, Endpoints.isEndpoint(value));
    }

    static Stream<Arguments> values() {
        return Stream.concat(
                Stream.of(Arguments.of("notAnEndpoint", false)),
                Arrays.stream(Endpoints.values()).map(Endpoints::name).map(n -> Arguments.of(n, true))
        );
    }

}