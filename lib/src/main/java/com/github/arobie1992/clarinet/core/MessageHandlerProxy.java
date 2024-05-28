package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;

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
    public None<Void> handle(Address remoteAddress, DataMessage message) {
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
            node.messageStore().add(message);
            userHandler.handle(remoteAddress, message);

            if(witness.equals(node.id())) {
                message.setWitnessSignature(node.genSignature(message.witnessParts()));
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
        public None<Void> handle(Address remoteAddress, DataMessage message) {
            return THE_NONE;
        }

        @Override
        public Class<DataMessage> inputType() {
            return DataMessage.class;
        }
    };
}
