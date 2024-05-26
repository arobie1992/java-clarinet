package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.message.MessageStore;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.Handler;
import com.github.arobie1992.clarinet.transport.Transport;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface NodeBuilder {
    NodeBuilder id(PeerId id);
    NodeBuilder peerStore(PeerStore peerStore);

    /**
     * Add the supplier to create the transport instance the node will use.
     * <p>
     * Because nodes may need to make modifications to the {@code Transport} to ensure protocol correctness,
     * this method accepts a {@code Supplier} that is subsequently used to instantiate the {@code Transport}
     * to discourage holding a reference to the transport independently of the node.
     * @param transportFactory A {@code Supplier} used to instantiate the {@code Transport} the node will use.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder transport(Supplier<Transport> transportFactory);

    /**
     * User-defined behavior for determining whether to accept a connection.
     * <p>
     * The returned {@code ConnectResponse} is used to determine if the node should create the connection or not. Users
     * do not need to perform any of these operations themselves. <b>This field is optional; however, if not specified,
     * all connections are accepted.</b>
     * @param connectHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder connectHandler(Handler<ConnectRequest, ConnectResponse> connectHandler);
    NodeBuilder trustFilter(Function<Stream<? extends Reputation>, Stream<PeerId>> trustFunction);
    NodeBuilder reputationStore(ReputationStore reputationStore);

    /**
     * User-defined behavior for determining whether to be witness to a connection.
     * <p>
     * The returned {@code WitnessResponse} is used to determine if the node should create the connection or not. Users
     * do not need to perform any of these operations themselves. <b>This field is optional; however, if not specified,
     * all witness requests are accepted.</b>
     * @param witnessHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder witnessHandler(Handler<WitnessRequest, WitnessResponse> witnessHandler);

    /**
     * User-defined behavior for dealing with witness notifications.
     * <p>
     * Users do not need to update the connection themselves.
     * @param witnessNotificationHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder witnessNotificationHandler(Handler<WitnessNotification, Void> witnessNotificationHandler);

    NodeBuilder messageStore(MessageStore messageStore);

    NodeBuilder keyStore(KeyStore keyStore);

    Node build();
}
