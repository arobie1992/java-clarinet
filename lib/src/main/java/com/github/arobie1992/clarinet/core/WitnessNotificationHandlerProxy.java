package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.transport.SendHandler;

import java.util.Objects;

class WitnessNotificationHandlerProxy implements SendHandler<WitnessNotification> {
    private final SendHandler<WitnessNotification> userHandler;
    private final ConnectionStore connectionStore;

    WitnessNotificationHandlerProxy(SendHandler<WitnessNotification> userHandler, ConnectionStore connectionStore) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
    }

    @Override
    public None<Void> handle(Address remoteAddress, WitnessNotification message) {
        userHandler.handle(remoteAddress, message);
        try(var ref = connectionStore.findForWrite(message.connectionId())) {
            switch (ref) {
                case Writeable(ConnectionImpl conn) -> {
                    if(!Connection.Status.AWAITING_WITNESS.equals(conn.status())) {
                        throw new UnsupportedOperationException("Connection " + message.connectionId() + " is not awaiting witness.");
                    }
                    conn.setWitness(message.witness());
                    conn.setStatus(Connection.Status.OPEN);
                }
                case Connection.Absent() -> throw new NoSuchConnectionException(message.connectionId());
            }
        }
        return null;
    }

    @Override
    public Class<WitnessNotification> inputType() {
        return userHandler.inputType();
    }

    private static final SendHandler<WitnessNotification> DEFAULT_HANDLER = new SendHandler<>() {

        @Override
        public None<Void> handle(Address remoteAddress, WitnessNotification message) {
            return null;
        }

        @Override
        public Class<WitnessNotification> inputType() {
            return WitnessNotification.class;
        }
    };
}
