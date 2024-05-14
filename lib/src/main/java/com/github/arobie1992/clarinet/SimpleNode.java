package com.github.arobie1992.clarinet;

import com.github.arobie1992.clarinet.connection.*;
import com.github.arobie1992.clarinet.data.MessageID;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerStore;
import com.github.arobie1992.clarinet.peer.PeersRequest;
import com.github.arobie1992.clarinet.peer.ReadOnlyPeer;
import com.github.arobie1992.clarinet.reputation.Reputation;
import com.github.arobie1992.clarinet.reputation.ReputationStore;
import com.github.arobie1992.clarinet.transport.Transport;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

class SimpleNode implements Node {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleNode.class);
    private final ConnectionStore connectionStore;
    private final Transport transport;
    private final Predicate<Reputation> trustFunc;
    private final ReputationStore reputationStore;
    private final PeerStore peerStore;

    SimpleNode(SimpleNodeBuilder builder) {
        connectionStore = builder.connectionStore;
        transport = builder.transport;
        trustFunc = builder.trustFunction;
        reputationStore = builder.reputationStore;
        peerStore = builder.peerStore;
    }

    @Override
    public List<Connection> connections() {
        return connectionStore.all().stream().map(id -> {
            var connRef = new AtomicReference<Connection>();
            connectionStore.read(id, conn -> connRef.set(ReadOnlyConnection.from(conn)));
            return connRef.get();
        }).toList();
    }

    @Override
    public List<Peer> peers() {
        return peerStore.all().stream().map(id -> {
            var ref = new AtomicReference<Peer>();
            peerStore.read(id, p -> ref.set(ReadOnlyPeer.from(p)));
            return ref.get();
        }).toList();
    }

    @Override
    public Peer self() {

    }

    @Override
    public void updatePeer(Peer peer) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ConnectionId connect(Peer peer, ConnectionOptions connectionOptions, TransportOptions transportOptions) {
        var connectionId = connectionStore.create(self(), peer, ConnectionStatus.REQUESTING_RECEIVER);
        var response = transport.exchange(
                peer,
                transportOptions,
                new ConnectRequest(connectionId, self().id(), peer.id(), connectionOptions),
                ConnectResponse.class
        );

        if(!connectionId.equals(response.connectionId())) {
            throw new UnexpectedConnectionException(connectionId, response.connectionId());
        }

        if(!response.errors().isEmpty()) {
            throw new ConnectErrorsException(connectionId, response.errors());
        }

        if(!response.accepted()) {
            throw new ConnectRejectedException(connectionId, response.rejectReasons());
        }

        connectionStore.update(connectionId, connection -> connection.updateStatus(ConnectionStatus.REQUESTING_WITNESS));
        var witness = peers().stream()
                .filter(p -> !(p.id().equals(self().id()) || p.id().equals(peer.id())))
                .filter(p -> {
                    var trusted = new AtomicBoolean(false);
                    reputationStore.read(p.id(), rep -> trusted.set(trustFunc.test(rep)));
                    return trusted.get();
                })
                .filter(c -> {
                    try {
                        var witnessResponse = transport.exchange(
                                peer,
                                transportOptions,
                                new WitnessRequest(connectionId, self().id(), peer.id(), connectionOptions),
                                WitnessResponse.class
                        );

                        if(!connectionId.equals(witnessResponse.connectionId())) {
                            throw new UnexpectedConnectionException(connectionId, witnessResponse.connectionId());
                        }

                        if(!witnessResponse.errors().isEmpty()) {
                            LOG.info("Witness candidate {} returned the following errors: {}", c.id(), witnessResponse.errors());
                            return false;
                        }

                        if(!witnessResponse.accepted()) {
                            LOG.info("Witness candidate {} rejected request for the following reasons: {}", c.id(), witnessResponse.rejectReasons());
                            return false;
                        }
                        return true;
                    } catch(RuntimeException e) {
                        LOG.warn("Error while requesting witness {} for connection {}", c.id(), connectionId, e);
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new WitnessSelectionException(connectionId));

        connectionStore.update(connectionId, connection -> connection.setWitness(witness).updateStatus(ConnectionStatus.NOTIFYING_OF_WITNESS));
        transport.send(peer, transportOptions, new WitnessNotification(connectionId, witness.id()));
        connectionStore.update(connectionId, connection -> connection.updateStatus(ConnectionStatus.OPEN));
        return connectionId;
    }

    @Override
    public void closeConnection(ConnectionId connectionId, TransportOptions transportOptions) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void requestPeers(Peer peer, PeersRequest request, TransportOptions transportOptions) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MessageID send(ConnectionId connectionID, TransportOptions transportOptions, byte[] data) {

    }
}
