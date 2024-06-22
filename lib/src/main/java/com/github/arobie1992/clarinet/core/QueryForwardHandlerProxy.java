package com.github.arobie1992.clarinet.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.message.QueryForward;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

class QueryForwardHandlerProxy implements SendHandler<QueryForward> {

    private final SendHandler<QueryForward> userHandler;
    private final ConnectionStore connectionStore;
    private final SimpleNode node;

    public QueryForwardHandlerProxy(SendHandler<QueryForward> userHandler, ConnectionStore connectionStore, SimpleNode node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public None<Void> handle(RemoteInformation remoteInformation, QueryForward message) {
        var resp = message.queryResponse();
        if(invalidSig(resp, remoteInformation.peer().id(), message.signature())) {
            var assessment = node.assessmentStore().find(remoteInformation.peer().id(), resp.messageDetails().messageId());
            node.assessmentStore().save(assessment.updateStatus(Assessment.Status.STRONG_PENALTY), node.reputationService()::update);
            userHandler.handle(remoteInformation, message);
            return new None<>();
        }
        if(invalidSig(resp.messageDetails(), message.queriedPeer(), resp.signature())) {
            var assessment = node.assessmentStore().find(remoteInformation.peer().id(), resp.messageDetails().messageId());
            node.assessmentStore().save(assessment.updateStatus(Assessment.Status.STRONG_PENALTY), node.reputationService()::update);
            userHandler.handle(remoteInformation, message);
            return new None<>();
        }

        List<PeerId> participants;
        try(var ref = connectionStore.findForRead(resp.messageDetails().messageId().connectionId())) {
            if(!(ref instanceof Connection.Readable(Connection connection))) {
                return new None<>();
            }
            participants = connection.participants();
        }

        var opt = node.messageStore().find(resp.messageDetails().messageId());
        if(opt.isEmpty()) {
            // right now, play it safe in case the message just didn't come through yet or something
            // reevaluate this later
            userHandler.handle(remoteInformation, message);
            return new None<>();
        }
        var expected = node.hash(opt.get().witnessParts(), resp.hashAlgorithm());
        var hash = resp.messageDetails().messageHash();
        if(Objects.equals(expected, hash)) {
            var assessment = node.assessmentStore().find(message.queriedPeer(), resp.messageDetails().messageId());
            node.assessmentStore().save(assessment.updateStatus(Assessment.Status.REWARD), node.reputationService()::update);
        } else {
            if(node.directCommunication(remoteInformation.peer().id(), participants)) {
                var assessment = node.assessmentStore().find(message.queriedPeer(), resp.messageDetails().messageId());
                node.assessmentStore().save(assessment.updateStatus(Assessment.Status.STRONG_PENALTY), node.reputationService()::update);
            } else {
                var assessment = node.assessmentStore().find(message.queriedPeer(), resp.messageDetails().messageId());
                node.assessmentStore().save(assessment.updateStatus(Assessment.Status.WEAK_PENALTY), node.reputationService()::update);
                var otherParticipant = node.getOtherParticipant(participants, remoteInformation.peer().id());
                var otherAssessment = node.assessmentStore().find(otherParticipant, resp.messageDetails().messageId());
                node.assessmentStore().save(otherAssessment.updateStatus(Assessment.Status.WEAK_PENALTY), node.reputationService()::update);
            }
        }
        userHandler.handle(remoteInformation, message);
        return new None<>();
    }

    private boolean invalidSig(Object data, PeerId peerId, Bytes signature) {
        try {
            return !node.checkSignature(data, peerId, signature);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Class<QueryForward> inputType() {
        return userHandler.inputType();
    }

    private static final SendHandler<QueryForward> DEFAULT_HANDLER = new SendHandler<>() {
        @Override
        public None<Void> handle(RemoteInformation remoteInformation, QueryForward message) {
            return new None<>();
        }
        @Override
        public Class<QueryForward> inputType() {
            return QueryForward.class;
        }
    };
}
