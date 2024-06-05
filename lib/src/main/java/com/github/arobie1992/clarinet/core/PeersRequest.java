package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Set;

/**
 * Request information on some number of peers from another node.
 * <p>
 * When any {@code PeerId}s are included in {@code requested}, information on these peers should be included
 * in the response unless the node to which the request was sent does not have information on them.
 * <p>
 * If {@code additionalRequested} is greater than 0, it indicates that the requestor wishes to have the requestee
 * provide that many additional peers of its own choosing. Requestees should do so, but may choose to respond with fewer
 * than the total requested; however, they must not provide more than {@code additionalRequested}. If a requestor
 * receives a response with more peers than {@code additionalRequested}, it is free to choose how it acts.
 *
 * @param requested Specific peers you wish to be included in the response.
 * @param additionalRequested The number of additional nodes to receive information on.
 */
public record PeersRequest(Set<PeerId> requested, int additionalRequested) {
    public PeersRequest {
        if(additionalRequested < 0) {
            throw new IllegalArgumentException("additionalRequested must be non-negative");
        }
    }
    public PeersRequest(Set<PeerId> requested) {
        this(requested, 0);
    }
    public PeersRequest(int numRequested) {
        this(Set.of(), numRequested);
    }
}
