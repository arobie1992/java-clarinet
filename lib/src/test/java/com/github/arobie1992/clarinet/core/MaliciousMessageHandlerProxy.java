package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;

public class MaliciousMessageHandlerProxy extends MessageHandlerProxy {
    private final ConnectionStore connectionStore;
    private final MaliciousNode node;

    MaliciousMessageHandlerProxy(
            SendHandler<DataMessage> userWitnessHandler,
            SendHandler<DataMessage> userReceiveHandler,
            ConnectionStore connectionStore,
            MaliciousNode node
    ) {
        super(userWitnessHandler, userReceiveHandler, connectionStore, node);
        this.connectionStore = connectionStore;
        this.node = node;
    }

    @Override
    public None<Void> handle(RemoteInformation remoteInformation, DataMessage message) {
        boolean isWitness;
        try(var ref = connectionStore.findForRead(message.messageId().connectionId())) {
            if(!(ref instanceof Connection.Readable(Connection connection))) {
                throw new NoSuchConnectionException(message.messageId().connectionId());
            }
            isWitness = connection.witness().map(id -> id.equals(node.id())).orElse(false);
        }
        if(isWitness && node.invalidSendSigOnMessage) {
            message.setSenderSignature(Bytes.of(new byte[]{106, 117, 110, 107}));
        }
        return super.handle(remoteInformation, message);
    }
}
