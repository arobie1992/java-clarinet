package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageDetails;
import com.github.arobie1992.clarinet.message.QueryRequest;
import com.github.arobie1992.clarinet.message.QueryResponse;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

public class MaliciousQueryHandlerProxy extends QueryHandlerProxy {
    private final MaliciousNode node;

    MaliciousQueryHandlerProxy(ExchangeHandler<QueryRequest, QueryResponse> userHandler, MaliciousNode node) {
        super(userHandler, node);
        this.node = node;
    }

    @Override
    public Some<QueryResponse> handle(RemoteInformation remoteInformation, QueryRequest message) {
        if(!node.configuration.queryAlterData().contains(remoteInformation.peer().id())
                && !node.configuration.queryBadSig().contains(remoteInformation.peer().id()))
        {
            return super.handle(remoteInformation, message);
        }

        var opt = node.messageStore().find(message.messageId());
        if(opt.isEmpty()) {
            return new Some<>(new QueryResponse(new MessageDetails(message.messageId(), null), null, null));
        }

        var storedMessage = opt.get();
        Bytes hash;
        if(node.configuration.queryAlterData().contains(remoteInformation.peer().id())) {
            var witnessParts = storedMessage.witnessParts();
            var bytes = witnessParts.data().bytes();
            bytes[0] += 1;
            var modified = new DataMessage.WitnessParts(witnessParts.messageId(), Bytes.of(bytes), witnessParts.senderSignature());
            hash = node.hash(modified, "SHA-256");
        } else {
            hash = node.hash(storedMessage.witnessParts(), "SHA-256");
        }
        var messageDetails = new MessageDetails(storedMessage.messageId(), hash);
        var sig = node.configuration.queryBadSig().contains(remoteInformation.peer().id())
            ? Bytes.of(new byte[]{7,7,7})
            : node.genSignature(messageDetails);
        return new Some<>(new QueryResponse(messageDetails, sig, "SHA-256"));
    }
}