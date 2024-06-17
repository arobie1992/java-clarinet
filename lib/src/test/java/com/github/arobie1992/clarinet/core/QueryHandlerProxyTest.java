package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.message.*;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryHandlerProxyTest {
    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final QueryRequest queryRequest = new QueryRequest(new MessageId(ConnectionId.random(), 0));
    private final QueryResponse expected = new QueryResponse(new byte[]{90}, new byte[]{47}, "SHA-256");
    private final DataMessage storedMessage = new DataMessage(queryRequest.messageId(), new byte[]{123});

    private ExchangeHandler<QueryRequest, QueryResponse> handler;
    private SimpleNode node;
    private QueryHandlerProxy proxy;
    private MessageStore messageStore;

    public QueryHandlerProxyTest() {
        storedMessage.setSenderSignature(new byte[]{22});
        storedMessage.setWitnessSignature(new byte[]{7});
    }

    @BeforeEach
    void setUp() {
        //noinspection unchecked
        handler = (ExchangeHandler<QueryRequest, QueryResponse>) mock(ExchangeHandler.class);
        node = mock(SimpleNode.class);
        proxy = new QueryHandlerProxy(null, node);
        messageStore = mock(MessageStore.class);
        when(node.messageStore()).thenReturn(messageStore);
        when(messageStore.find(queryRequest.messageId())).thenReturn(Optional.of(storedMessage));
        when(node.genSignature(expected.hash())).thenReturn(expected.signature());
    }

    @Test
    void testHonorsUserHandler() {
        proxy = new QueryHandlerProxy(handler, node);
        var expected = new Some<>(new QueryResponse(null,null, null));
        when(handler.handle(remoteInformation, queryRequest)).thenReturn(expected);
        assertEquals(expected, proxy.handle(remoteInformation, queryRequest));
    }

    @Test
    void testUserHandlerReturnsNull() {
        proxy = new QueryHandlerProxy(handler, node);
        when(handler.handle(remoteInformation, queryRequest)).thenReturn(null);
        var ex = assertThrows(NullPointerException.class, () -> proxy.handle(remoteInformation, queryRequest));
        assertEquals("userHandler returned null", ex.getMessage());
    }

    @Test
    void testNoStoredMessage() {
        when(messageStore.find(queryRequest.messageId())).thenReturn(Optional.empty());
        var expected = new Some<>(new QueryResponse(null,null, null));
        assertEquals(expected, proxy.handle(remoteInformation, queryRequest));
    }

    @Test
    void testHasMessage() {
        when(node.hash(storedMessage.witnessParts(), expected.hashAlgorithm())).thenReturn(expected.hash());
        assertEquals(expected, proxy.handle(remoteInformation, queryRequest).value());
    }

    @Test
    void testInputType() {
        assertEquals(QueryRequest.class, proxy.inputType());
    }
}