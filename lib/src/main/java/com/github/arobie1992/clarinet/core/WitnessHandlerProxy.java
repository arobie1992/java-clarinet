package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

import java.util.Objects;

class WitnessHandlerProxy implements ExchangeHandler<WitnessRequest, WitnessResponse> {
    private final ExchangeHandler<WitnessRequest, WitnessResponse> userHandler;
    private final ConnectionStore connectionStore;
    private final Node node;

    WitnessHandlerProxy(ExchangeHandler<WitnessRequest, WitnessResponse> userHandler, ConnectionStore connectionStore, Node node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public Some<WitnessResponse> handle(RemoteInformation remoteInformation, WitnessRequest message) {
        var peer = node.peerStore().find(remoteInformation.peer().id()).orElse(remoteInformation.peer());
        peer.addresses().addAll(remoteInformation.peer().addresses());
        node.peerStore().save(peer);

        var resp = Objects.requireNonNull(userHandler.handle(remoteInformation, message), "User handler returned a null WitnessResponse");
        if(!resp.value().rejected()) {
            connectionStore.accept(message.connectionId(), message.sender(), message.receiver(), Connection.Status.OPEN);
            try(var ref = connectionStore.findForWrite(message.connectionId())) {
                switch (ref) {
                    // setWitness automatically persists back to the reference so no need to do any saving
                    case Writeable(ConnectionImpl conn) -> conn.setWitness(node.id());
                    case Connection.Absent() -> throw new IllegalStateException("Failed to accept connection");
                }
            }
        }

        return resp;
    }

    @Override
    public Class<WitnessRequest> inputType() {
        return userHandler.inputType();
    }

    private static final ExchangeHandler<WitnessRequest, WitnessResponse> DEFAULT_HANDLER = new ExchangeHandler<>() {
        @Override
        public Some<WitnessResponse> handle(RemoteInformation remoteInformation, WitnessRequest message) {
            return new Some<>(new WitnessResponse(false, null));
        }

        @Override
        public Class<WitnessRequest> inputType() {
            return WitnessRequest.class;
        }
    };
}
