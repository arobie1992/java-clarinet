package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.Handler;

import java.util.Objects;

class WitnessHandlerProxy implements Handler<WitnessRequest, WitnessResponse> {
    private final Handler<WitnessRequest, WitnessResponse> userHandler;
    private final ConnectionStore connectionStore;
    private final Node node;

    WitnessHandlerProxy(Handler<WitnessRequest, WitnessResponse> userHandler, ConnectionStore connectionStore, Node node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public WitnessResponse handle(Address remoteAddress, WitnessRequest message) {
        var resp = Objects.requireNonNull(userHandler.handle(remoteAddress, message), "User handler returned a null WitnessResponse");
        if(!resp.rejected()) {
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

    private static final Handler<WitnessRequest, WitnessResponse> DEFAULT_HANDLER = new Handler<>() {
        @Override
        public WitnessResponse handle(Address address, WitnessRequest message) {
            return new WitnessResponse(false, null);
        }

        @Override
        public Class<WitnessRequest> inputType() {
            return WitnessRequest.class;
        }
    };
}
