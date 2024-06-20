package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.message.MessageForward;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

class MessageForwardHandlerProxy implements SendHandler<MessageForward> {
    private static final None<Void> THE_NONE = new None<>();
    private static final Logger log = LoggerFactory.getLogger(MessageForwardHandlerProxy.class);

    private final SendHandler<MessageForward> userHandler;
    private final ConnectionStore connectionStore;
    private final SimpleNode node;

    MessageForwardHandlerProxy(SendHandler<MessageForward> userHandler, ConnectionStore connectionStore, SimpleNode node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
    }

    private static class HandlerException extends RuntimeException {
        public HandlerException(String message) {
            super("Received message forward " + message);
        }
    }

    @Override
    public None<Void> handle(RemoteInformation remoteInformation, MessageForward message) {
        var receiver = remoteInformation.peer().id();
        var connectionId = message.summary().messageId().connectionId();
        try {
            PeerId recId;
            PeerId witId;
            try(var ref = connectionStore.findForRead(connectionId)) {
                if(!(ref instanceof Connection.Readable(Connection connection))) {
                    throw new HandlerException("from " + receiver + " for a message on connection "
                            + connectionId + " that node does not have record of");
                }
                if(!connection.receiver().equals(receiver)) {
                    throw new HandlerException("from " + receiver + "who is not receiver on connection" + connectionId);
                }
                if(!node.id().equals(connection.sender())) {
                    throw new HandlerException("for a message on connection " + connectionId + " that node was not sender for");
                }
                witId = connection.witness().orElseThrow(() ->
                        new HandlerException("for a message on connection " + connectionId + " that does not have witness"));
                recId = connection.receiver();
            }

            if(!node.checkSignatureHash(message.summary().hash(), witId, message.summary().witnessSignature())) {
                // receivers should only ever send this if the witness signature is valid
                var assessment = node.assessmentStore().find(recId, message.summary().messageId());
                node.assessmentStore().save(assessment.updateStatus(Assessment.Status.STRONG_PENALTY), node.reputationService()::update);
                throw new HandlerException("for a message on connection " + connectionId + " that has invalid witness signature");
            }

            // TODO need to review protocol and see where this stands
            // not currently using receiver signature because it doesn't contribute to the protocol,
            // but have it there in case it's helpful later on

            var storedMsg = node.messageStore().find(message.summary().messageId());
            if(storedMsg.isEmpty()) {
                throw new HandlerException("for a message on connection " + connectionId + " that there is no record of");
            }

            var hash = node.hash(storedMsg.get().witnessParts(), message.summary().hashAlgorithm());
            if(!Objects.equals(hash, message.summary().hash())) {
                var assessment = node.assessmentStore().find(witId, message.summary().messageId());
                node.assessmentStore().save(assessment.updateStatus(Assessment.Status.STRONG_PENALTY), node.reputationService()::update);
            }
            userHandler.handle(remoteInformation, message);
        } catch(HandlerException e) {
            log.warn(e.getMessage());
        }
        return THE_NONE;
    }

    @Override
    public Class<MessageForward> inputType() {
        return userHandler.inputType();
    }

    private static final SendHandler<MessageForward> DEFAULT_HANDLER = new SendHandler<>() {
        @Override
        public None<Void> handle(RemoteInformation remoteInformation, MessageForward message) {
            return THE_NONE;
        }
        @Override
        public Class<MessageForward> inputType() {
            return MessageForward.class;
        }
    };
}
