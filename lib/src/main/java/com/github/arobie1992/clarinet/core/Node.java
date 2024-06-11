package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.crypto.KeyStore;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.message.MessageStore;
import com.github.arobie1992.clarinet.message.QueryResult;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;

public interface Node {
    PeerId id();
    PeerStore peerStore();
    Transport transport();

    ReputationStore reputationStore();

    Connection.ReadableReference findConnection(ConnectionId connectionId);

    MessageStore messageStore();

    KeyStore keyStore();

    /**
     * Establishes an outgoing communication channel with the specified node at the specified address.
     * <p>
     * If the node does not have an already-existing peer corresponding to {@code peerId}, it adds a peer with this ID.
     * If the peer does not have the specified address, the peer is updated to include this address. The returned
     * {@code ConnectionId} must always be non-null; if an implementation wishes to signal failures, it should do so
     * through throwing exceptions.
     *
     * @param receiver The peer ID of the receiver and the address at which it will be contacted for this connection.
     * @param connectionOptions Any connection configuration. See {@link ConnectionOptions} and specific implementation
     *                          notes for supported options.
     * @param transportOptions Any configuration used for the underlying transport protocol. See {@link TransportOptions}
     *                         and specific implementation notes for supported options.
     * @return the {@link ConnectionId} of the newly created connection.
     */
    ConnectionId connect(PeerId receiver, ConnectionOptions connectionOptions, TransportOptions transportOptions);

    MessageId send(ConnectionId connectionId, byte[] data, TransportOptions transportOptions);

    /**
     * Query the given peer for the message and check if it matches the expectation.
     * <p>
     * This method does not make any reputation updates. If you wish to perform a reputation update based on the query
     * result, use {@link Node#updateReputation(QueryResult)}. This is done to allow for flexibility in querying that
     * falls outside the core Clarinet semantics. Some examples are:
     * <ul>
     *     <li>Querying nodes that were not participants in the connection.</li>
     *     <li>Querying for messages this node does not have a record of.</li>
     *     <li>Allowing for latency between sending the message and the peer receiving it.</li>
     * </ul>
     * @param peerId The unique identifier of the peer you wish to query.
     * @param messageId The unique identifier of the message to query the peer for.
     * @param transportOptions Any configuration used for the underlying transport protocol.
     *                         See {@link TransportOptions} and specific implementation notes for supported options.
     * @return a {@link QueryResult} with information regarding the query and response.
     */
    QueryResult query(PeerId peerId, MessageId messageId, TransportOptions transportOptions);

    /**
     * Apply an update to the reputation based on the Clarinet semantics.
     * <p>
     * The detailed semantics are as follows:
     * <ol>
     *     <li>If the {@code QueryResponse} in the provided {@code QueryResult} has an invalid signature, a strong
     *     penalty is applied to the queried peer.</li>
     *     <li>If the queried peer was not a participant in the connection, no reputation update occurs.</li>
     *     <li>If this node does not have a record of the queried message, no reputation update occurs.</li>
     *     <li>If the contents of the {@code QueryResponse} do not match the expected:</li>
     *     <ul>
     *         <li>And this node communicated directly with the queried peer in the connection, a strong penalty is
     *         applied to the queried peer.</li>
     *         <li>And this node did not communicate directly with the queried peer in the connection, a weak penalty is
     *         applied to the queried peer and the peer through which the two communicated.</li>
     *     </ul>
     *     <li>The contents of the {@code QueryResponse} match the expected, a reward is applied to the queried peer.</li>
     * </ol>
     * The first match in the above list is the action taken, and further evaluation does not occur.
     * @param queryResult The {@link QueryResult} to determine how reputation should be updated.
     * @return {@code true} if a reputation update is performed; {@code false} otherwise.
     */
    boolean updateReputation(QueryResult queryResult);

    PeersResponse requestPeers(PeerId requestee, PeersRequest request, TransportOptions transportOptions);

    KeysResponse requestKeys(PeerId requestee, KeysRequest request, TransportOptions transportOptions);

    void addConnectHandler(ExchangeHandler<ConnectRequest, ConnectResponse> connectHandler);

    void removeConnectHandler();

    void addWitnessRequestHandler(ExchangeHandler<WitnessRequest, WitnessResponse> witnessHandler);

    void addWitnessNotificationHandler(SendHandler<WitnessNotification> witnessNotificationHandler);

    void removeWitnessRequestHandler();

    void removeWitnessNotificationHandler();

    void addPeersRequestHandler(ExchangeHandler<PeersRequest, PeersResponse> peersRequestHandler);

    void removePeersRequestHandler();

    void addMessageHandler(SendHandler<DataMessage> messageHandler);

    void removeMessageHandler();

    void addKeysRequestHandler(ExchangeHandler<KeysRequest, KeysResponse> keysRequestHandler);

    void removeKeysRequestHandler();
}
