package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.RawKey;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeysRequestHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final KeysRequest request = new KeysRequest();
    private final RawKey rawKey = new RawKey("testAlgorithm", new byte[]{83});
    private final Some<KeysResponse> expected = new Some<>(new KeysResponse(List.of(rawKey)));

    private ExchangeHandler<KeysRequest, KeysResponse> handler;
    private Node node;
    private KeysRequestHandlerProxy proxy;

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        handler = (ExchangeHandler<KeysRequest, KeysResponse>) mock(ExchangeHandler.class);
        node = mock(Node.class);
        when(node.id()).thenReturn(PeerUtils.senderId());
        proxy = new KeysRequestHandlerProxy(null, node);

        var keyStore = mock(KeyStore.class);
        when(node.keyStore()).thenReturn(keyStore);
        var key = mock(PublicKey.class);
        when(keyStore.findPublicKeys(node.id())).thenReturn(List.of(key));
        when(key.algorithm()).thenReturn(rawKey.algorithm());
        when(key.bytes()).thenReturn(rawKey.bytes());
    }

    @Test
    void testNullNode() {
        assertThrows(NullPointerException.class, () -> new KeysRequestHandlerProxy(null, null));
    }

    @Test
    void testUserHandlerHonored() {
        proxy = new KeysRequestHandlerProxy(handler, node);
        when(handler.handle(remoteInformation, request)).thenReturn(expected);
        var actual = proxy.handle(remoteInformation, request);
        assertEquals(expected, actual);
        verify(node, never()).keyStore();
    }

    @Test
    void testUserHandlerReturnsNull() {
        proxy = new KeysRequestHandlerProxy(handler, node);
        when(handler.handle(remoteInformation, request)).thenReturn(null);
        var ex = assertThrows(NullPointerException.class, () -> proxy.handle(remoteInformation, request));
        assertEquals("userHandler returned null", ex.getMessage());
    }

    @Test
    void testDefaultBehavior() {
        var resp = proxy.handle(remoteInformation, request);
        assertEquals(expected, resp);
    }

    @Test
    void testInputType() {
        assertEquals(KeysRequest.class, proxy.inputType());
    }

}