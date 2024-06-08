package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.PublicKeyProvider;
import com.github.arobie1992.clarinet.impl.netty.ConnectionIdSerializer;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class MessageHandlerProxy implements SendHandler<DataMessage> {
    private static final Logger log = LoggerFactory.getLogger(MessageHandlerProxy.class);
    private static final None<Void> THE_NONE = new None<>();

    private final SendHandler<DataMessage> userHandler;
    private final ConnectionStore connectionStore;
    private final SimpleNode node;
    private final ObjectMapper objectMapper = new ObjectMapper();

    MessageHandlerProxy(SendHandler<DataMessage> userHandler, ConnectionStore connectionStore, SimpleNode node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
        var module = new SimpleModule();
        module.addSerializer(ConnectionId.class, new ConnectionIdSerializer());
        objectMapper.registerModule(module);
    }

    @Override
    public None<Void> handle(RemoteInformation remoteInformation, DataMessage message) {
        var peer = node.peerStore().find(remoteInformation.peer().id()).orElse(remoteInformation.peer());
        peer.addresses().addAll(remoteInformation.peer().addresses());
        node.peerStore().save(peer);

        var connectionId = message.messageId().connectionId();
        try(var ref = connectionStore.findForWrite(connectionId)) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new NoSuchConnectionException(connectionId);
            }
            if(!connection.status().equals(Connection.Status.OPEN)) {
                throw new ConnectionStatusException(connectionId, "send", connection.status(), List.of(Connection.Status.OPEN));
            }
            var witness = connection.witness().orElseThrow();
            if(!connection.receiver().equals(node.id()) && !witness.equals(node.id())) {
                throw new IllegalArgumentException("Connection is not through or to " + node.id());
            }

            try {
                if(node.id().equals(witness)) {
                    handleAsWitness(remoteInformation, connection, message);
                } else {
                    handleAsReceiver(remoteInformation, connection, witness, message);
                }
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }
        return THE_NONE;
    }

    private void handleAsWitness(RemoteInformation remoteInformation, Connection connection, DataMessage message) throws JsonProcessingException {
        message.setWitnessSignature(node.genSignature(message.witnessParts()));
        node.messageStore().add(message);

        var rep = node.reputationStore().find(connection.sender());
        if(checkSignature(message.senderParts(), connection.sender(), message.senderSignature())) {
            rep.reward();
        } else {
            rep.strongPenalize();
        }
        node.reputationStore().save(rep);

        userHandler.handle(remoteInformation, message);
        // TODO add ability for user to set transport options for handler
        node.sendInternal(connection.receiver(), message, new TransportOptions());
    }

    private void handleAsReceiver(RemoteInformation remoteInformation, Connection connection, PeerId witness, DataMessage message) throws JsonProcessingException {
        node.messageStore().add(message);

        var witRep = node.reputationStore().find(witness);
        // TODO have receiver forward message to sender if either signature is invalid
        if(checkSignature(message.witnessParts(), witness, message.witnessSignature())) {
            var sendRep = node.reputationStore().find(connection.sender());
            if(checkSignature(message.senderParts(), connection.sender(), message.senderSignature())) {
                witRep.reward();
                sendRep.reward();
            } else {
                witRep.weakPenalize();
                sendRep.weakPenalize();
            }
            node.reputationStore().save(sendRep);
        } else {
            witRep.strongPenalize();
        }
        node.reputationStore().save(witRep);

        userHandler.handle(remoteInformation, message);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean checkSignature(Object parts, PeerId peerId, Optional<byte[]> signature) throws JsonProcessingException {
        var data = objectMapper.writeValueAsBytes(parts);
        var keys = getOrLoadKeys(peerId);
        return signature.map(sig -> keys.stream()
                        .map(k -> {
                            try {
                                return k.verify(data, sig);
                            } catch (RuntimeException e) {
                                log.debug("Encountered error for key {}", k, e);
                                return false;
                            }
                        })
                        .filter(v -> v)
                        .findFirst()
                        .orElse(false))
                .orElse(false);
    }

    private Collection<PublicKey> getOrLoadKeys(PeerId peerId) {
        var keys = node.keyStore().findPublicKeys(peerId);
        if(keys.isEmpty()) {
            keys = node.requestKeys(peerId, new KeysRequest(), new TransportOptions()).keys()
                    .stream()
                    .map(k -> node.keyStore().providers()
                                .filter(p -> p instanceof PublicKeyProvider)
                                .map(p -> (PublicKeyProvider) p)
                                .filter(p -> p.supports(k.algorithm()))
                                .map(p -> {
                                    try {
                                        return p.create(k.bytes());
                                    } catch (RuntimeException e) {
                                        log.debug("Encountered error creating key for RawKey {}", k.algorithm(), e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .findFirst())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            keys.forEach(k -> node.keyStore().addPublicKey(peerId, k));
        }
        return keys;
    }

    @Override
    public Class<DataMessage> inputType() {
        return userHandler.inputType();
    }

    private static final SendHandler<DataMessage> DEFAULT_HANDLER = new SendHandler<>() {
        @Override
        public None<Void> handle(RemoteInformation remoteInformation, DataMessage message) {
            return THE_NONE;
        }
        @Override
        public Class<DataMessage> inputType() {
            return DataMessage.class;
        }
    };

}
