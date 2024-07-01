package com.github.arobie1992.clarinet.sampleapp;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.adt.None;
import com.github.arobie1992.clarinet.core.*;
import com.github.arobie1992.clarinet.impl.crypto.KeyProviders;
import com.github.arobie1992.clarinet.impl.crypto.Keys;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryAssessmentStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryKeyStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryMessageStore;
import com.github.arobie1992.clarinet.impl.inmemory.InMemoryPeerStore;
import com.github.arobie1992.clarinet.impl.netty.NettyTransport;
import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.impl.peer.UriAddress;
import com.github.arobie1992.clarinet.impl.reputation.ProportionalReputationService;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.Address;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.query.QueryTerms;
import com.github.arobie1992.clarinet.reputation.TrustFilters;
import com.github.arobie1992.clarinet.transport.RemoteInformation;
import com.github.arobie1992.clarinet.transport.SendHandler;
import com.github.arobie1992.clarinet.transport.TransportOptions;
import com.github.arobie1992.clarinet.transport.UncheckedURISyntaxException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.arobie1992.clarinet.query.Orderings.random;
import static com.github.arobie1992.clarinet.sampleapp.ThreadUtils.await;
import static com.github.arobie1992.clarinet.core.Connection.Status.*;

public class TestNode implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TestNode.class);

    private final Node node;

    private final CollectionReference peers;
    private final CollectionReference reputations;

    private final int rounds = 10;
    private record Action(String name, Runnable action) {}
    private final List<Action> actions = List.of(
            new Action("connect", this::connect),
            new Action("send", this::send),
            new Action("requestPeers", this::requestPeers),
            new Action("query", this::query),
            new Action("close", this::close),
            new Action("idle", this::idle)
    );
    private record PeerMessage(PeerId peerId, MessageId messageId) {}
    private final List<PeerMessage> queryCandidates = new ArrayList<>();
    private final Object candidateLock = new Object();

    public TestNode() throws NoSuchAlgorithmException {
        var id = new StringPeerId(UUID.randomUUID().toString());
        node = Nodes.newBuilder()
                .id(id)
                .peerStore(new InMemoryPeerStore())
                .transport(() -> new NettyTransport(id, new TransportOptions()))
                .trustFilter(TrustFilters.minAndStandardDeviation(0.5))
                .assessmentStore(new InMemoryAssessmentStore())
                .reputationService(new ProportionalReputationService())
                .messageStore(new InMemoryMessageStore())
                .keyStore(new InMemoryKeyStore())
                .build();
        node.keyStore().addKeyPair(node.id(), Keys.generateKeyPair());
        node.keyStore().addProvider(KeyProviders.Sha256RsaPublicKeyProvider());
        var recordCandidateHandler = new SendHandler<DataMessage>() {
            @Override
            public None<Void> handle(RemoteInformation remoteInformation, DataMessage message) {
                List<PeerId> otherParticipants;
                try(var ref = node.findConnection(message.messageId().connectionId())) {
                    if(!(ref instanceof Connection.Readable(Connection conn))) {
                        throw new NoSuchConnectionException(message.messageId().connectionId());
                    }
                    otherParticipants = conn.participants().stream().filter(p -> !p.equals(node.id())).toList();
                }
                synchronized (candidateLock) {
                    otherParticipants.forEach(id -> queryCandidates.add(new PeerMessage(id, message.messageId())));
                }
                return new None<>();
            }
            @Override
            public Class<DataMessage> inputType() {
                return DataMessage.class;
            }
        };
        node.addWitnessHandler(recordCandidateHandler);
        node.addReceiveHandler(recordCandidateHandler);

        FirestoreOptions firestoreOptions;
        try {
            firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId("yoonlab")
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var db = firestoreOptions.getService();
        var trial = db.collection("trial");
        var trialSnapshot = await(trial.orderBy("value", Query.Direction.DESCENDING).limit(1).get());
        var latestTrial = trialSnapshot.getDocuments().getFirst().get("value", int.class);
        peers = db.collection("peers" + latestTrial);
        reputations = db.collection("reputations" + latestTrial);
//        TODO // add loading config
    }

    @Override
    public void run() {
        var addr = getAddr();
        node.transport().add(addr);
        register(addr);
        seedPeers();
        for(int i = 0; i < rounds; i++) {
//            ThreadUtils.uncheckedSleep(Duration.ofSeconds(Rand.in(0, 5)));
//            var action = actions.get(Rand.in(0, actions.size() - 1));
//            try {
//                action.action.run();
//            } catch(RuntimeException e) {
//                log.error("Encountered error during {}", action.name, e);
//            }
        }
        recordPeerReputations();
        node.transport().shutdown();
    }

    private Address getAddr() {
        try {
            // If this doesn't work, refer to here:
            // https://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
            var addr = InetAddress.getLocalHost();
            var ipAddr = addr.getHostAddress();
            // just use an ephemeral port
            return new UriAddress(new URI("tcp://" + ipAddr + ":0"));
        } catch (UnknownHostException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        }
    }

    private void register(Address address) {
        var ref = peers.document(node.id().asString());
        var result = ref.set(Map.of("addressUri", address.asURI().toString()));
        await(result);
    }

    private Peer toPeer(DocumentReference document) {
        DocumentSnapshot snapshot = await(document.get());
        var peerId = new StringPeerId(snapshot.getId());
        var uriStr = Objects.requireNonNull(snapshot.get("addressUri")).toString();
        Address address;
        try {
            address = new UriAddress(new URI(uriStr));
        } catch (URISyntaxException e) {
            throw new UncheckedURISyntaxException(e);
        }
        return new Peer(peerId, Set.of(address));
    }

    private void seedPeers() {
        int required = 5;
        var found = new HashSet<Peer>();
        outer: for(int i = 0; i < 10; i++) {
            var itr = peers.listDocuments();
            var list = new ArrayList<DocumentReference>();
            itr.forEach(list::add);
            Collections.shuffle(list);
            for(var doc : list) {
                var peer = toPeer(doc);
                if(!peer.id().equals(node.id())) {
                    found.add(peer);
                    if(found.size() == required) {
                        break outer;
                    }
                }
            }
            ThreadUtils.uncheckedSleep(Duration.ofSeconds(5));
        }
        if(found.size() < required) {
            log.warn("Was unable to seed desired number of peers. Wanted: {}, Got: {}", required, found.size());
        } else {
            log.info("Successfully seed desired number of peers: {}", found.size());
        }
        found.forEach(peer -> node.peerStore().save(peer));
    }

    private PeerId randomPeer() {
        var candidates = node.peerStore().all().collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(candidates);
        return candidates.getFirst();
    }

    private void connect() {
        PeerId receiver = randomPeer();
        var connectionOptions = new ConnectionOptions(Rand.flip() ? node.id() : receiver);
        node.connect(receiver, connectionOptions, new TransportOptions());
    }

    private void send() {
        var raw = new byte[Rand.in(1000, 10_000)];
        for(int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Rand.in(0, 255);
        }
        var data = new Bytes(raw);
        Predicate<Connection> openOutgoing = c -> c.status().equals(OPEN) && c.sender().equals(node.id());
        node.queryConnections(new QueryTerms<>(openOutgoing, random())).findFirst().ifPresent(r -> {
            try(r) {
                var connection = r.connection();
                var messageId = node.send(connection.id(), data, new TransportOptions());
                synchronized (candidateLock) {
                    queryCandidates.add(new PeerMessage(connection.witness().orElseThrow(), messageId));
                    queryCandidates.add(new PeerMessage(connection.receiver(), messageId));
                }
            }
        });
    }

    private void requestPeers() {
        PeerId requestee = randomPeer();
        var request = new PeersRequest(Rand.in(1, 10));
        var response = node.requestPeers(requestee, request, new TransportOptions());
        response.peers().forEach(p -> {
            var toSave = node.peerStore().find(p.id()).map(s -> {
                s.addresses().addAll(p.addresses());
                return s;
            }).orElse(p);
            node.peerStore().save(toSave);
        });
    }

    private void query() {
        PeerMessage chosen;
        synchronized (candidateLock) {
            var index = Rand.in(0, queryCandidates.size() - 1);
            chosen = queryCandidates.remove(index);
        }
        var result = node.query(chosen.peerId, chosen.messageId, new TransportOptions());
        node.processQueryResult(result, new TransportOptions());
    }

    private void close() {
        node.queryConnections(new QueryTerms<>(c -> c.status().equals(CLOSED), random()))
                .findFirst()
                .ifPresent(r -> {
                    try(r) {
                        node.close(r.connection().id(), new CloseOptions(), new TransportOptions());
                    }
                });
    }

    private void idle() {
        ThreadUtils.uncheckedSleep(Duration.ofSeconds(Rand.in(0, 5)));
    }

    private void recordPeerReputations() {
        var peerReputations = node.peerStore().all()
                .map(p -> Map.entry(p.asString(), node.reputationService().get(p)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var rep = reputations.document(node.id().asString());
        await(rep.set(peerReputations));
    }
}
