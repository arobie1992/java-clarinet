package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.crypto.PublicKey;
import com.github.arobie1992.clarinet.crypto.PublicKeyProvider;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class MessageHandlerProxy implements SendHandler<DataMessage> {
    private static final None<Void> THE_NONE = new None<>();

    private final SendHandler<DataMessage> userHandler;
    private final ConnectionStore connectionStore;
    private final SimpleNode node;

    MessageHandlerProxy(SendHandler<DataMessage> userHandler, ConnectionStore connectionStore, SimpleNode node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
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

            getOrLoadKeys(connection.sender());

            // have the witness sign the message prior to saving it
            if(witness.equals(node.id())) {
                message.setWitnessSignature(node.genSignature(message.witnessParts()));
            } else {
                getOrLoadKeys(witness);
            }
            node.messageStore().add(message);
            userHandler.handle(remoteInformation, message);

            if(witness.equals(node.id())) {
                // TODO add ability for user to set transport options for handler
                var transportOptions = new TransportOptions(Optional.empty(), Optional.empty());
                node.sendInternal(connection.receiver(), message, transportOptions);
            }
        }
        return THE_NONE;
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
                                    return p.create(k.keyBytes());
                                } catch (RuntimeException e) {
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

}
