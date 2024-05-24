package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.TransportUtils;
import com.github.arobie1992.clarinet.transport.Handler;
import com.github.arobie1992.clarinet.transport.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransportProxyTest {

    private Transport transport;
    private TransportProxy transportProxy;

    @BeforeEach
    public void setUp() {
        transport = mock(Transport.class);
        transportProxy = new TransportProxy(transport);
    }

    @Test
    void testAddAddress() {
        when(transport.add(AddressUtils.defaultAddress())).thenReturn(AddressUtils.defaultAddress());
        assertEquals(AddressUtils.defaultAddress(), transportProxy.add(AddressUtils.defaultAddress()));
    }

    @Test
    void testRemoveAddress() {
        when(transport.remove(AddressUtils.defaultAddress())).thenReturn(Optional.of(AddressUtils.defaultAddress()));
        assertEquals(Optional.of(AddressUtils.defaultAddress()), transportProxy.remove(AddressUtils.defaultAddress()));
    }

    @Test
    void testAddresses() {
        when(transport.addresses()).thenReturn(List.of(AddressUtils.defaultAddress()));
        assertEquals(List.of(AddressUtils.defaultAddress()), transportProxy.addresses());
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void testAddHandler(String endpoint) {
        var handler = mock(Handler.class);
        if(Endpoints.isEndpoint(endpoint)) {
            var ex = assertThrows(IllegalArgumentException.class, () -> transportProxy.add(endpoint, handler));
            assertEquals("Please use the node-level operations for altering protocol-required endpoint: " + endpoint, ex.getMessage());
        } else {
            assertDoesNotThrow(() -> transportProxy.add(endpoint, handler));
            verify(transport).add(endpoint, handler);
        }
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void testAddInternal(String endpoint) {
        var handler = mock(Handler.class);
        assertDoesNotThrow(() -> transportProxy.addInternal(endpoint, handler));
        verify(transport).add(endpoint, handler);
    }

    @ParameterizedTest
    @MethodSource("endpoints")
    void testRemoveHanlder(String endpoint) {
        if(Endpoints.isEndpoint(endpoint)) {
            var ex = assertThrows(IllegalArgumentException.class, () -> transportProxy.remove(endpoint));
            assertEquals("Please use the node-level operations for altering protocol-required endpoint: " + endpoint, ex.getMessage());
        } else {
            var handler = mock(Handler.class);
            //noinspection unchecked
            when(transport.remove(endpoint)).thenReturn(Optional.of(handler));
            var removed = assertDoesNotThrow(() -> transportProxy.remove(endpoint));
            assertEquals(Optional.of(handler), removed);
        }
    }

    private static Stream<Arguments> endpoints() {
        return Stream.concat(
                Stream.of(Arguments.of("someOtherEndpoint")),
                Arrays.stream(Endpoints.values()).map(Endpoints::name).map(Arguments::of)
        );
    }

    @Test
    void testEndpoints() {
        var endpoints = List.of("testEndpoint");
        when(transport.endpoints()).thenReturn(endpoints);
        assertEquals(endpoints, transportProxy.endpoints());
    }

    @Test
    void testExchange() {
        var endpoint = "endpoint";
        var msg = "msg";
        var resp = "Test response";
        when(transport.exchange(AddressUtils.defaultAddress(), endpoint, msg, String.class, TransportUtils.defaultOptions())).thenReturn(resp);
        assertEquals(resp, transportProxy.exchange(AddressUtils.defaultAddress(), endpoint, msg, String.class, TransportUtils.defaultOptions()));
    }

    @Test
    void testShutdown() {
        transportProxy.shutdown();
        verify(transport).shutdown();
    }
}