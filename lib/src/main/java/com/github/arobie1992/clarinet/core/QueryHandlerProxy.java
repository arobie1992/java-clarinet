package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.message.MessageDetails;
import com.github.arobie1992.clarinet.message.QueryRequest;
import com.github.arobie1992.clarinet.message.QueryResponse;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

import java.util.Objects;

class QueryHandlerProxy implements ExchangeHandler<QueryRequest, QueryResponse> {
    private static final String HASH_ALG = "SHA-256";

    private final ExchangeHandler<QueryRequest, QueryResponse> userHandler;
    private final SimpleNode node;

    QueryHandlerProxy(ExchangeHandler<QueryRequest, QueryResponse> userHandler, SimpleNode node) {
        this.userHandler = userHandler;
        this.node = node;
    }

    @Override
    public Some<QueryResponse> handle(RemoteInformation remoteInformation, QueryRequest message) {
        if(userHandler != null) {
            return Objects.requireNonNull(userHandler.handle(remoteInformation, message), "userHandler returned null");
        }

        var opt = node.messageStore().find(message.messageId());
        if(opt.isEmpty()) {
            return new Some<>(new QueryResponse(new MessageDetails(message.messageId(), null), null, null));
        }

        var storedMessage = opt.get();
        var hash = node.hash(storedMessage.witnessParts(), HASH_ALG);
        var messageDetails = new MessageDetails(storedMessage.messageId(), hash);
        var sig = node.genSignature(messageDetails);
        return new Some<>(new QueryResponse(messageDetails, sig, HASH_ALG));
    }

    @Override
    public Class<QueryRequest> inputType() {
        return QueryRequest.class;
    }
}
