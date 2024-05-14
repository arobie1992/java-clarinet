package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.connection.ConnectionStore;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.Transport;

import java.util.function.Predicate;

public interface NodeBuilder {
   NodeBuilder trustFunction(Predicate<Reputation> trustFunction);
   NodeBuilder transport(Transport transport);
   NodeBuilder connectionStore(ConnectionStore connectionStore);
   NodeBuilder reputationStore(ReputationStore reputationStore);
   NodeBuilder peerStore(PeerStore peerStore);
   Node build();
}
