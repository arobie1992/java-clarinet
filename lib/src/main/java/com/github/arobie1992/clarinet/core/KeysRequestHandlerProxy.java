package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.crypto.RawKey;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

import java.util.Objects;

class KeysRequestHandlerProxy implements ExchangeHandler<KeysRequest, KeysResponse> {
    private final ExchangeHandler<KeysRequest, KeysResponse> userHandler;
    private final Node node;

    public KeysRequestHandlerProxy(ExchangeHandler<KeysRequest, KeysResponse> userHandler, Node node) {
        this.userHandler = userHandler;
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public Some<KeysResponse> handle(RemoteInformation remoteInformation, KeysRequest message) {
        if(userHandler != null) {
            return Objects.requireNonNull(userHandler.handle(remoteInformation, message), "userHandler returned null");
        }
        var keys = node.keyStore().findPublicKeys(node.id()).stream().map(k -> new RawKey(k.algorithm(), k.bytes())).toList();
        return new Some<>(new KeysResponse(keys));
    }

    @Override
    public Class<KeysRequest> inputType() {
        return KeysRequest.class;
    }
}
