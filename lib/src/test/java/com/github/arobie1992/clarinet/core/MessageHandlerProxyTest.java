package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.PublicKeyProvider;
import com.github.arobie1992.clarinet.crypto.RawKey;
import com.github.arobie1992.clarinet.impl.netty.ConnectionIdSerializer;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.message.MessageStore;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.testutils.AddressUtils;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageHandlerProxyTest {

    private final RemoteInformation remoteInformation = new RemoteInformation(
            new Peer(PeerUtils.senderId(), new HashSet<>(Set.of(AddressUtils.defaultAddress()))),
            AddressUtils.defaultAddress()
    );
    private final byte[] senderSignature = {22};
    private final byte[] witnessSignature = {45};
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DataMessage message;
    private SendHandler<DataMessage> userHandler;
    private ConnectionStore connectionStore;
    private SimpleNode node;
    private MessageHandlerProxy proxy;
    private ConnectionImpl connection;
    private MessageStore messageStore;
    private PeerStore peerStore;

    private KeyStore keyStore;
    private PublicKey senderKey;
    private PublicKey witnessKey;
    private PublicKeyProvider provider;

    private Reputation senderRep;
    private Reputation witnessRep;
    private ReputationStore reputationStore;

    MessageHandlerProxyTest() {
        var module = new SimpleModule();
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        message = new DataMessage(new MessageId(ConnectionId.random(), 0), new byte[]{77, 50, 126});
        message.setSenderSignature(senderSignature);
        //noinspection unchecked
        userHandler = (SendHandler<DataMessage>) mock(SendHandler.class);
        connectionStore = mock(ConnectionStore.class);
        node = mock(SimpleNode.class);
        proxy = new MessageHandlerProxy(null, connectionStore, node);

        connection = new ConnectionImpl(message.messageId().connectionId(), PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        // this gets thrown away after each test so it should be fine
        connection.lock.writeLock().lock();
        connection.setWitness(PeerUtils.witnessId());
        when(connectionStore.findForWrite(message.messageId().connectionId())).thenReturn(new Writeable(connection));

        when(node.id()).thenReturn(connection.witness().orElseThrow());
        // don't know why the mock isn't working when I pass the witness parts (probably a dumb mistake), so do this for now.
        // does mean we need to be careful about calling genSignature
        when(node.genSignature(any())).thenReturn(witnessSignature);

        messageStore = mock(MessageStore.class);
        when(node.messageStore()).thenReturn(messageStore);

        peerStore = mock(PeerStore.class);
        when(node.peerStore()).thenReturn(peerStore);
        when(peerStore.find(remoteInformation.peer().id())).thenReturn(Optional.empty());

        keyStore = mock(KeyStore.class);
        when(node.keyStore()).thenReturn(keyStore);
        senderKey = mock(PublicKey.class);
        witnessKey = mock(PublicKey.class);
        when(keyStore.findPublicKeys(connection.sender())).thenReturn(List.of(senderKey));
        when(keyStore.findPublicKeys(connection.witness().orElseThrow())).thenReturn(List.of(witnessKey));
        provider = mock(PublicKeyProvider.class);
        // Have to use an answer because the exact stream instance gets used every time otherwise and throws an already consumed error.
        when(keyStore.providers()).then(im -> Stream.of(provider));
        when(provider.supports(any())).thenReturn(true);

        reputationStore = mock(ReputationStore.class);
        when(node.reputationStore()).thenReturn(reputationStore);
        senderRep = mock(Reputation.class);
        witnessRep = mock(Reputation.class);
        when(reputationStore.find(connection.sender())).thenReturn(senderRep);
        when(reputationStore.find(connection.witness().orElseThrow())).thenReturn(witnessRep);

        when(senderKey.verify(objectMapper.writeValueAsBytes(message.senderParts()), senderSignature)).thenReturn(true);
    }

    @Test
    void testNullConnectionStore() {
        assertThrows(NullPointerException.class, () -> new MessageHandlerProxy(userHandler, null, node));
    }

    @Test
    void testNullNode() {
        assertThrows(NullPointerException.class, () -> new MessageHandlerProxy(userHandler, connectionStore, null));
    }

    @Test
    void testConnectionAbsent() {
        when(connectionStore.findForWrite(message.messageId().connectionId())).thenReturn(new Connection.Absent());
        var ex = assertThrows(NoSuchConnectionException.class, () -> proxy.handle(remoteInformation, message));
        assertEquals(message.messageId().connectionId(), ex.connectionId());
    }

    @Test
    void testConnectionNotOpen() {
        connection.setStatus(Connection.Status.REQUESTING_RECEIVER);
        var ex = assertThrows(ConnectionStatusException.class, () -> proxy.handle(remoteInformation, message));
        assertEquals(message.messageId().connectionId(), ex.connectionId());
        assertEquals("send", ex.operation());
        assertEquals(Connection.Status.REQUESTING_RECEIVER, ex.status());
        assertEquals(List.of(Connection.Status.OPEN), ex.permittedStatuses());
    }

    @Test
    void testConnectionNoWitness() {
        connection = new ConnectionImpl(message.messageId().connectionId(), PeerUtils.senderId(), PeerUtils.receiverId(), Connection.Status.OPEN);
        connection.lock.writeLock().lock();
        when(connectionStore.findForWrite(message.messageId().connectionId())).thenReturn(new Writeable(connection));
        assertThrows(NoSuchElementException.class, () -> proxy.handle(remoteInformation, message));
    }

    @Test
    void testNodeNotWitnessOrReceiver() {
        when(node.id()).thenReturn(PeerUtils.senderId());
        var ex = assertThrows(IllegalArgumentException.class, () -> proxy.handle(remoteInformation, message));
        assertEquals("Connection is not through or to " + node.id(), ex.getMessage());
    }

    @Test
    void testDefaultHandlerInputType() {
        assertEquals(DataMessage.class, proxy.inputType());
    }

    @Test
    void testWitnessSignatureValid() {
        proxy.handle(remoteInformation, message);
        assertArrayEquals(witnessSignature, message.witnessSignature().orElseThrow());
        verify(messageStore).add(message);
        verify(senderRep).reward();
        verify(reputationStore).save(senderRep);
        verify(node).sendInternal(connection.receiver(), message, new TransportOptions());
    }

    @Test
    void testWitnessSignatureInvalid() throws JsonProcessingException {
        when(senderKey.verify(objectMapper.writeValueAsBytes(message.senderParts()), senderSignature)).thenReturn(false);
        proxy.handle(remoteInformation, message);
        assertArrayEquals(witnessSignature, message.witnessSignature().orElseThrow());
        verify(messageStore).add(message);
        verify(senderRep).strongPenalize();
        verify(reputationStore).save(senderRep);
        verify(node).sendInternal(connection.receiver(), message, new TransportOptions());
    }

    @Test
    void testWitnessRetrievesKeys() throws JsonProcessingException {
        when(keyStore.findPublicKeys(connection.sender())).thenReturn(List.of());

        var failsCreationRawKey = new RawKey("failing", new byte[]{1});
        var failsVerifyingRawKey = new RawKey("failing2", new byte[]{2});
        var successfulRawKey = new RawKey("successful", new byte[]{3});
        var failingKey = mock(PublicKey.class);
        var resp = new KeysResponse(List.of(failsCreationRawKey, failsVerifyingRawKey, successfulRawKey));

        when(node.requestKeys(connection.sender(), new KeysRequest(), new TransportOptions())).thenReturn(resp);
        when(provider.create(failsCreationRawKey.bytes())).thenThrow(RuntimeException.class);
        when(provider.create(failsVerifyingRawKey.bytes())).thenReturn(failingKey);
        when(provider.create(successfulRawKey.bytes())).thenReturn(senderKey);

        when(failingKey.verify(objectMapper.writeValueAsBytes(message.senderParts()), message.senderSignature().orElseThrow()))
                .thenThrow(RuntimeException.class);

        proxy.handle(remoteInformation, message);
        verify(keyStore).addPublicKey(connection.sender(), senderKey);
        assertArrayEquals(witnessSignature, message.witnessSignature().orElseThrow());
        verify(messageStore).add(message);
        verify(senderRep).reward();
        verify(reputationStore).save(senderRep);
        verify(node).sendInternal(connection.receiver(), message, new TransportOptions());
    }

    @Test
    void testWitnessUserHandlerCalled() {
        proxy = new MessageHandlerProxy(userHandler, connectionStore, node);
        proxy.handle(remoteInformation, message);
        verify(userHandler).handle(remoteInformation, message);
    }

    @Test
    void testReceiverSignaturesValid() throws JsonProcessingException {
        when(node.id()).thenReturn(connection.receiver());
        message.setWitnessSignature(witnessSignature);
        when(witnessKey.verify(objectMapper.writeValueAsBytes(message.witnessParts()), witnessSignature)).thenReturn(true);
        proxy.handle(remoteInformation, message);
        verify(messageStore).add(message);
        verify(senderRep).reward();
        verify(witnessRep).reward();
        verify(reputationStore).save(senderRep);
        verify(reputationStore).save(witnessRep);
    }

    @Test
    void testReceiverWitnessSignatureInvalid() throws JsonProcessingException {
        when(node.id()).thenReturn(connection.receiver());
        message.setWitnessSignature(witnessSignature);
        when(witnessKey.verify(objectMapper.writeValueAsBytes(message.witnessParts()), witnessSignature)).thenReturn(false);
        proxy.handle(remoteInformation, message);
        verify(messageStore).add(message);
        verifyNoInteractions(senderRep);
        verify(witnessRep).strongPenalize();
        verify(reputationStore).save(witnessRep);
    }

    @Test
    void testReceiverSenderSignatureInvalid() throws JsonProcessingException {
        when(node.id()).thenReturn(connection.receiver());
        message.setWitnessSignature(witnessSignature);
        when(witnessKey.verify(objectMapper.writeValueAsBytes(message.witnessParts()), witnessSignature)).thenReturn(true);
        when(senderKey.verify(objectMapper.writeValueAsBytes(message.senderParts()), senderSignature)).thenReturn(false);
        proxy.handle(remoteInformation, message);
        verify(messageStore).add(message);
        verify(senderRep).weakPenalize();
        verify(witnessRep).weakPenalize();
        verify(reputationStore).save(senderRep);
        verify(reputationStore).save(witnessRep);
    }

    @Test
    void testReceiverRetrievesKeys() throws JsonProcessingException {
        when(node.id()).thenReturn(connection.receiver());
        message.setWitnessSignature(witnessSignature);
        // just use the same keys for both because I'm lazy
        when(keyStore.findPublicKeys(connection.sender())).thenReturn(List.of());
        when(keyStore.findPublicKeys(connection.witness().orElseThrow())).thenReturn(List.of());

        var failsCreationRawKey = new RawKey("failing", new byte[]{1});
        var failsVerifyingRawKey = new RawKey("failing2", new byte[]{2});
        var successfulSenderRawKey = new RawKey("successful", new byte[]{3});
        var successfulWitnessRawKey = new RawKey("successful", new byte[]{4});
        var failingKey = mock(PublicKey.class);
        var resp = new KeysResponse(List.of(failsCreationRawKey, failsVerifyingRawKey, successfulSenderRawKey));
        var witResp = new KeysResponse(List.of(failsCreationRawKey, failsVerifyingRawKey, successfulWitnessRawKey));

        when(node.requestKeys(connection.sender(), new KeysRequest(), new TransportOptions())).thenReturn(resp);
        when(node.requestKeys(connection.witness().orElseThrow(), new KeysRequest(), new TransportOptions())).thenReturn(witResp);
        when(provider.create(failsCreationRawKey.bytes())).thenThrow(RuntimeException.class);
        when(provider.create(failsVerifyingRawKey.bytes())).thenReturn(failingKey);
        when(provider.create(successfulSenderRawKey.bytes())).thenReturn(senderKey);
        when(provider.create(successfulWitnessRawKey.bytes())).thenReturn(witnessKey);

        when(failingKey.verify(objectMapper.writeValueAsBytes(message.senderParts()), senderSignature)).thenThrow(RuntimeException.class);
        when(failingKey.verify(objectMapper.writeValueAsBytes(message.witnessParts()), witnessSignature)).thenThrow(RuntimeException.class);
        when(witnessKey.verify(objectMapper.writeValueAsBytes(message.witnessParts()), witnessSignature)).thenReturn(true);

        proxy.handle(remoteInformation, message);
        verify(keyStore).addPublicKey(connection.sender(), senderKey);
        verify(keyStore).addPublicKey(connection.witness().orElseThrow(), witnessKey);
        verify(messageStore).add(message);
        verify(senderRep).reward();
        verify(witnessRep).reward();
        verify(reputationStore).save(senderRep);
        verify(reputationStore).save(witnessRep);
    }

    @Test
    void testReceiverUserHandlerCalled() {
        when(node.id()).thenReturn(connection.receiver());
        proxy = new MessageHandlerProxy(userHandler, connectionStore, node);
        proxy.handle(remoteInformation, message);
        verify(userHandler).handle(remoteInformation, message);
    }

}