package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageForward;
import com.github.arobie1992.clarinet.message.MessageSummary;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

class MessageHandlerProxy implements SendHandler<DataMessage> {
    private static final None<Void> THE_NONE = new None<>();
    private static final String HASH_ALG = "SHA-256";

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

        var assessment = node.assessmentStore().find(connection.sender(), message.messageId());
        var status = node.checkSignature(message.senderParts(), connection.sender(), message.senderSignature())
                ? Assessment.Status.REWARD
                : Assessment.Status.STRONG_PENALTY;
        node.assessmentStore().save(assessment.updateStatus(status), node.reputationService()::update);

        userHandler.handle(remoteInformation, message);
        // TODO add ability for user to set transport options for handler
        node.sendInternal(connection.receiver(), message, new TransportOptions());
    }

    private void handleAsReceiver(RemoteInformation remoteInformation, Connection connection, PeerId witness, DataMessage message) throws JsonProcessingException {
        node.messageStore().add(message);

        var witAssessment = node.assessmentStore().find(witness, message.messageId());
        if(node.checkSignature(message.witnessParts(), witness, message.witnessSignature())) {
            var sendAssessment = node.assessmentStore().find(connection.sender(), message.messageId());
            if(node.checkSignature(message.senderParts(), connection.sender(), message.senderSignature())) {
                witAssessment = witAssessment.updateStatus(Assessment.Status.REWARD);
                sendAssessment = sendAssessment.updateStatus(Assessment.Status.REWARD);
            } else {
                witAssessment = witAssessment.updateStatus(Assessment.Status.WEAK_PENALTY);
                sendAssessment = sendAssessment.updateStatus(Assessment.Status.WEAK_PENALTY);
                /* FIXME this isn't going to work in a real impl because the witness could've used a different hashing algorithm
                   but should work for this prototype; long term would probably be to have the nodes agree upon a hash algorithm
                   as part of the connection */
                var hash = node.hash(message.witnessParts(), HASH_ALG);
                var summary = new MessageSummary(message.messageId(), hash, HASH_ALG, message.witnessSignature().orElseThrow());
                var sig = node.genSignature(summary);
                var sender = node.peerStore().find(connection.sender()).orElseThrow(() -> new NoSuchPeerException(connection.sender()));
                node.sendForPeer(sender, Endpoints.MESSAGE_FORWARD.name(), new MessageForward(summary, sig), new TransportOptions());
            }
            node.assessmentStore().save(sendAssessment, node.reputationService()::update);
        } else {
            witAssessment = witAssessment.updateStatus(Assessment.Status.STRONG_PENALTY);
        }
        node.assessmentStore().save(witAssessment, node.reputationService()::update);

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
