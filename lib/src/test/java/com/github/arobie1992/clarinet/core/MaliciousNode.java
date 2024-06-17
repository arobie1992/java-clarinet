package com.github.arobie1992.clarinet.core;

public class MaliciousNode extends SimpleNode {
    public boolean invalidSendSigOnMessage = true;
    public MaliciousNode(MaliciuosNodeBuilder builder) {
        super(builder);
        transport.addInternal(Endpoints.MESSAGE.name(), new MaliciousMessageHandlerProxy(builder.messageHandler, connectionStore, this));
    }

    public static class MaliciuosNodeBuilder extends Builder {
        @Override
        public MaliciousNode build() {
            return new MaliciousNode(this);
        }
    }
}
