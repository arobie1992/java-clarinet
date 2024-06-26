package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.message.*;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.AssessmentStore;
import com.github.arobie1992.clarinet.reputation.ReputationService;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.Transport;

import java.util.function.BiFunction;
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
    NodeBuilder assessmentStore(AssessmentStore assessmentStore);
    NodeBuilder messageStore(MessageStore messageStore);
    NodeBuilder keyStore(KeyStore keyStore);
    NodeBuilder trustFilter(BiFunction<Stream<? extends PeerId>, Function<PeerId, Double>, Stream<? extends PeerId>> trustFunction);

    /**
     * User-defined behavior for determining whether to accept a connection.
     * <p>
     * The returned {@code ConnectResponse} is used to determine if the node should create the connection or not. Users
     * do not need to perform any of these operations themselves. <b>This field is optional; however, if not specified,
     * all connections are accepted.</b>
     * @param connectHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder connectHandler(ExchangeHandler<ConnectRequest, ConnectResponse> connectHandler);

    NodeBuilder reputationService(ReputationService reputationService);

    /**
     * User-defined behavior for determining whether to be witness to a connection.
     * <p>
     * The returned {@code WitnessResponse} is used to determine if the node should create the connection or not. Users
     * do not need to perform any of these operations themselves. <b>This field is optional; however, if not specified,
     * all witness requests are accepted.</b>
     * @param witnessRequestHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder witnessRequestHandler(ExchangeHandler<WitnessRequest, WitnessResponse> witnessRequestHandler);

    /**
     * User-defined behavior for dealing with witness notifications.
     * <p>
     * Users do not need to update the connection themselves.
     * @param witnessNotificationHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder witnessNotificationHandler(SendHandler<WitnessNotification> witnessNotificationHandler);

    /**
     * User-defined behavior for messages. This handler is called when a node serves as the witness in a connection.
     * @param witnessHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder witnessHandler(SendHandler<DataMessage> witnessHandler);

    /**
     * User-defined behavior for messages. This handler is called when a node serves as the receiver in a connection.
     * @param receiveHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder receiveHandler(SendHandler<DataMessage> receiveHandler);

    /**
     * User behavior for how to respond to a {@link PeersRequest}.
     * <p>
     * No provided handler means the system should construct the response to the best of its ability. By default, the
     * node itself that received the {@code PeersRequest} will not include information on itself unless it is
     * specifically included in {@link PeersRequest#requested()} as the assumption is that if the node is able to be
     * contacted the requestor already had information on it.
     * <p>
     * Providing a handler means that the system will return the user-provided response exactly as is.
     * @param peersRequestHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder peersRequestHandler(ExchangeHandler<PeersRequest, PeersResponse> peersRequestHandler);

    /**
     * User behavior for how to respond to a {@link KeysRequest}.
     * <p>
     * No provided handler means the system should construct the response to the best of its ability. Providing a
     * handler means that the system will return the user-provided response exactly as is.
     *
     * @param keysRequestHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder keysRequestHandler(ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler);

    /**
     * User behavior for how to respond to a {@link QueryRequest}.
     * <p>
     * No provided handler means the system should construct the response to the best of its ability. Providing a
     * handler means that the system will return the user-provided response exactly as is.
     *
     * @param queryHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder queryHandler(ExchangeHandler<QueryRequest, QueryResponse> queryHandler);

    /**
     * User-defined behavior for dealing with close requests.
     * <p>
     * Users do not need to update the connection themselves.
     * @param closeHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder closeHandler(SendHandler<CloseRequest> closeHandler);

    /**
     * User-defined behavior for dealing with message forwards.
     * <p>
     * Users do not need to perform reputation updates themselves.
     * @param messageForwardHandler The handler implementing the user-desired behavior.
     * @return {@code this} builder for fluent building.
     */
    NodeBuilder messageForwardHandler(SendHandler<MessageForward> messageForwardHandler);

    NodeBuilder queryForwardHandler(SendHandler<QueryForward> queryForwardHandler);

    Node build();
}
