package com.github.arobie1992.clarinet.testutils;

import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.peer.ReadOnlyPeer;

public class PeerUtils {
    private PeerUtils() {}

    private static final Peer SENDER = new ReadOnlyPeer(new TestPeerId(), "sender");
    private static final Peer WITNESS = new ReadOnlyPeer(new TestPeerId(), "witness");
    private static final Peer RECEIVER = new ReadOnlyPeer(new TestPeerId(), "receiver");

    public static Peer sender() {
        return SENDER;
    }

    public static Peer witness() {
        return WITNESS;
    }

    public static Peer receiver() {
        return RECEIVER;
    }
}
