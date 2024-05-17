package com.github.arobie1992.clarinet.testutils;

import com.github.arobie1992.clarinet.impl.peer.StringPeerId;
import com.github.arobie1992.clarinet.peer.PeerId;

public class PeerUtils {

    private static final PeerId senderId = new StringPeerId("sender");
    private static final PeerId witnessId = new StringPeerId("witness");
    private static final PeerId receiverId = new StringPeerId("receiver");

    public static PeerId senderId() {
        return senderId;
    }

    public static PeerId witnessId() {
        return witnessId;
    }

    public static PeerId receiverId() {
        return receiverId;
    }

}
