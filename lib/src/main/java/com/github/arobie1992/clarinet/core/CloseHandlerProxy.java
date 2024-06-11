package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;

import java.util.Objects;

class CloseHandlerProxy implements SendHandler<CloseRequest> {
    private static final None<Void> THE_NONE = new None<>();

    private final SendHandler<CloseRequest> userHandler;
    private final ConnectionStore connectionStore;

    CloseHandlerProxy(SendHandler<CloseRequest> userHandler, ConnectionStore connectionStore) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
    }

    @Override
    public None<Void> handle(RemoteInformation remoteInformation, CloseRequest message) {
        userHandler.handle(remoteInformation, message);
        try(var ref = connectionStore.findForWrite(message.connectionId())) {
            if(!(ref instanceof Writeable(ConnectionImpl connection))) {
                throw new NoSuchConnectionException(message.connectionId());
            }
            connection.setStatus(Connection.Status.CLOSED);
        }
        return THE_NONE;
    }

    @Override
    public Class<CloseRequest> inputType() {
        return userHandler.inputType();
    }

    private static final SendHandler<CloseRequest> DEFAULT_HANDLER = new SendHandler<>() {
        @Override
        public None<Void> handle(RemoteInformation remoteInformation, CloseRequest message) {
            return THE_NONE;
        }
        @Override
        public Class<CloseRequest> inputType() {
            return CloseRequest.class;
        }
    };
}
