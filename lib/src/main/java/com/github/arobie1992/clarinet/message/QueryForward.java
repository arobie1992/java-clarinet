package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Objects;

public record QueryForward(PeerId queriedPeer, QueryResponse queryResponse, Bytes signature) {
    public QueryForward {
        Objects.requireNonNull(queriedPeer);
        Objects.requireNonNull(queryResponse);
        Objects.requireNonNull(signature);
    }
}
