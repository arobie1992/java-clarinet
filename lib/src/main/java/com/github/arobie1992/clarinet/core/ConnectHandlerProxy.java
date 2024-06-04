package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

import java.util.Objects;

class ConnectHandlerProxy implements ExchangeHandler<ConnectRequest, ConnectResponse> {
    private final ExchangeHandler<ConnectRequest, ConnectResponse> userHandler;
    private final ConnectionStore connectionStore;
    private final Node node;

    ConnectHandlerProxy(ExchangeHandler<ConnectRequest, ConnectResponse> userHandler, ConnectionStore connectionStore, Node node) {
        this.userHandler = userHandler == null ? DEFAULT_HANDLER : userHandler;
        this.connectionStore = Objects.requireNonNull(connectionStore);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public Some<ConnectResponse> handle(RemoteInformation remoteInformation, ConnectRequest message) {
        var peer = node.peerStore().find(remoteInformation.peer().id()).orElse(remoteInformation.peer());
        peer.addresses().addAll(remoteInformation.peer().addresses());
        node.peerStore().save(peer);

        var resp = Objects.requireNonNull(userHandler.handle(remoteInformation, message), "User handler returned a null ConnectResponse");
        if(!resp.value().rejected()) {
            connectionStore.accept(message.connectionId(), message.sender(), node.id(), Connection.Status.AWAITING_WITNESS);
        }

        return resp;
    }

    @Override
    public Class<ConnectRequest> inputType() {
        return userHandler.inputType();
    }

    private static final ExchangeHandler<ConnectRequest, ConnectResponse> DEFAULT_HANDLER = new ExchangeHandler<>() {
        @Override
        public Some<ConnectResponse> handle(RemoteInformation remoteInformation, ConnectRequest message) {
            return new Some<>(new ConnectResponse(false, null));
        }

        @Override
        public Class<ConnectRequest> inputType() {
            return ConnectRequest.class;
        }
    };
}
