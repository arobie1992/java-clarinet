package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.Handler;

import java.util.Objects;

class WitnessNotificationHandlerProxy implements Handler<WitnessNotification, Void> {
    private final Handler<WitnessNotification, Void> userHandler;
    private final ConnectionStore connectionStore;

    WitnessNotificationHandlerProxy(Handler<WitnessNotification, Void> userHandler, ConnectionStore connectionStore) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
    }

    @Override
    public Void handle(Address remoteAddress, WitnessNotification message) {
        userHandler.handle(remoteAddress, message);
        try(var ref = connectionStore.findForWrite(message.connectionId())) {
            switch (ref) {
                case Writeable(ConnectionImpl conn) -> {
                    conn.setWitness(message.witness());
                    conn.setStatus(Connection.Status.OPEN);
                }
                case Connection.Absent ignored -> throw new NoSuchConnectionException(message.connectionId());
            }
        }
        return null;
    }

    @Override
    public Class<WitnessNotification> inputType() {
        return userHandler.inputType();
    }

    private static final Handler<WitnessNotification, Void> DEFAULT_HANDLER = new Handler<>() {

        @Override
        public Void handle(Address remoteAddress, WitnessNotification message) {
            return null;
        }

        @Override
        public Class<WitnessNotification> inputType() {
            return WitnessNotification.class;
        }
    };
}
