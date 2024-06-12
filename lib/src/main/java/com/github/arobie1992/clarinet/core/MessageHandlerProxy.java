package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

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
        if(node.checkSignature(message.senderParts(), connection.sender(), message.senderSignature())) {
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
        // TODO have receiver forward message to sender if sender signature is invalid
        if(node.checkSignature(message.witnessParts(), witness, message.witnessSignature())) {
            var sendRep = node.reputationStore().find(connection.sender());
            if(node.checkSignature(message.senderParts(), connection.sender(), message.senderSignature())) {
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
