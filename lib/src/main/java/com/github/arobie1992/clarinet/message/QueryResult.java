package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Objects;

public record QueryResult(PeerId queriedPeer, MessageId queriedMessage, QueryResponse queryResponse) {
    public QueryResult {
        Objects.requireNonNull(queriedPeer);
        Objects.requireNonNull(queriedMessage);
        Objects.requireNonNull(queryResponse);
    }
}
