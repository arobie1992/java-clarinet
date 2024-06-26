package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.transport.TransportOptions;

import java.util.List;

public class MaliciousNode extends SimpleNode {
    final Configuration configuration;
    public MaliciousNode(Builder builder) {
        super(builder);
        this.configuration = builder.configuration;
        transport.addInternal(
                Endpoints.MESSAGE.name(),
                new MaliciousMessageHandlerProxy(builder.witnessHandler, builder.receiveHandler, connectionStore, this)
        );
        transport.addInternal(Endpoints.QUERY.name(), new MaliciousQueryHandlerProxy(builder.queryHandler, this));
    }

    @Override
    void sendInternal(PeerId peerId, DataMessage message, TransportOptions transportOptions) {
        if(configuration.sendBadSig()) {
            var isSender = false;
            try(var ref = super.connectionStore.findForRead(message.messageId().connectionId())) {
                if(!(ref instanceof Connection.Readable(Connection connection))) {
                    throw new NoSuchConnectionException(message.messageId().connectionId());
                }
                isSender = connection.sender().equals(id());
            }
            if(isSender) {
                var bytes = message.senderSignature().map(Bytes::bytes).orElse(new byte[]{6, 5, 4});
                bytes[0] += 1;
                message.setSenderSignature(Bytes.of(bytes));
            }
        }
        super.sendInternal(peerId, message, transportOptions);
    }

    public static class Builder extends SimpleNode.Builder {
        private final Configuration configuration;

        public Builder(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public MaliciousNode build() {
            return new MaliciousNode(this);
        }
    }

    public record Configuration(
            boolean sendBadSig,
            boolean witnessAlterData,
            boolean witnessBadSig,
            boolean messageForwardAlterData,
            boolean messageForwardBadSig,
            List<PeerId> queryAlterData,
            List<PeerId> queryBadSig,
            List<PeerId> queryForwardAlterData,
            List<PeerId> queryForwardBadSig
    ) {
        public Configuration {
            queryAlterData = queryAlterData == null ? List.of() : queryAlterData;
            queryBadSig = queryBadSig == null ? List.of() : queryBadSig;
            queryForwardAlterData = queryForwardAlterData == null ? List.of() : queryForwardAlterData;
            queryForwardBadSig = queryForwardBadSig == null ? List.of() : queryForwardBadSig;
        }

        public static Builder builder() {
            return new Builder();
        }

        // there's nothing wrong with using the constructor; this is to make only setting specific arguments easier
        // without having to have every possible constructor permutation
        public static class Builder {
            boolean sendBadSig;
            boolean witnessAlterData;
            boolean witnessBadSig;
            boolean messageForwardAlterData;
            boolean messageForwardBadSig;
            List<PeerId> queryAlterData;
            List<PeerId> queryBadSig;
            List<PeerId> queryForwardAlterData;
            List<PeerId> queryForwardBadSig;

            public Builder sendBadSig(boolean sendBadSig) {
                this.sendBadSig = sendBadSig;
                return this;
            }

            public Builder witnessAlterData(boolean witnessAlterData) {
                this.witnessAlterData = witnessAlterData;
                return this;
            }

            public Builder witnessBadSig(boolean witnessBadSig) {
                this.witnessBadSig = witnessBadSig;
                return this;
            }

            public Builder messageForwardAlterData(boolean messageForwardAlterData) {
                this.messageForwardAlterData = messageForwardAlterData;
                return this;
            }

            public Builder messageForwardBadSig(boolean messageForwardBadSig) {
                this.messageForwardBadSig = messageForwardBadSig;
                return this;
            }

            public Builder queryAlterData(List<PeerId> queryAlterData) {
                this.queryAlterData = queryAlterData;
                return this;
            }

            public Builder queryBadSig(List<PeerId> queryBadSig) {
                this.queryBadSig = queryBadSig;
                return this;
            }

            public Builder queryForwardAlterData(List<PeerId> queryForwardAlterData) {
                this.queryForwardAlterData = queryForwardAlterData;
                return this;
            }

            public Builder queryForwardBadSig(List<PeerId> queryForwardBadSig) {
                this.queryForwardBadSig = queryForwardBadSig;
                return this;
            }

            public Configuration build() {
                return new Configuration(
                        sendBadSig,
                        witnessAlterData,
                        witnessBadSig,
                        messageForwardAlterData,
                        messageForwardBadSig,
                        queryAlterData,
                        queryBadSig,
                        queryForwardAlterData,
                        queryForwardBadSig
                );
            }
        }
    }
}
