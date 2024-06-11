package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.message.QueryRequest;
import com.github.arobie1992.clarinet.message.QueryResponse;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

import java.util.Objects;

class QueryHandlerProxy implements ExchangeHandler<QueryRequest, QueryResponse> {
    private static final String HASH_ALG = "SHA-256";

    private final ExchangeHandler<QueryRequest, QueryResponse> userHandler;
    private final ConnectionStore connectionStore;
    private final SimpleNode node;

    QueryHandlerProxy(ExchangeHandler<QueryRequest, QueryResponse> userHandler, ConnectionStore connectionStore, SimpleNode node) {
        this.userHandler = userHandler;
        this.connectionStore = connectionStore;
        this.node = node;
    }

    @Override
    public Some<QueryResponse> handle(RemoteInformation remoteInformation, QueryRequest message) {
        if(userHandler != null) {
            return Objects.requireNonNull(userHandler.handle(remoteInformation, message), "userHandler returned null");
        }

        var opt = node.messageStore().find(message.messageId());
        if(opt.isEmpty()) {
            return new Some<>(new QueryResponse(null, null, null));
        }

        var storedMessage = opt.get();
        PeerId sender;
        try(var ref = connectionStore.findForRead(message.messageId().connectionId())) {
            if(!(ref instanceof Connection.Readable(Connection connection))) {
                throw new NoSuchConnectionException(message.messageId().connectionId());
            }
            sender = connection.sender();
        }
        Object parts = sender.equals(node.id()) || sender.equals(remoteInformation.peer().id())
                ? storedMessage.senderParts()
                : storedMessage.witnessParts();
        var hash = node.hash(parts, HASH_ALG);
        var sig = node.genSignature(hash);
        return new Some<>(new QueryResponse(hash, sig, HASH_ALG));
    }

    @Override
    public Class<QueryRequest> inputType() {
        return QueryRequest.class;
    }
}
