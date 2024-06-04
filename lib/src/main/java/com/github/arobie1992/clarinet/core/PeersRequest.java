package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.ArrayList;
import java.util.List;

/**
 * Request information on some number of peers from another node.
 * <p>
 * When any {@code PeerId}s are included {@code requested} in requested, information on these peers should be included
 * in the response unless the node to which the request was sent does not have information on them.
 * <p>
 * If {@code num} is greater than the number of IDs in {@code requested} it indicates that the requestor wishes to have
 * the requestee provide the difference in additional peers of its own choosing. Requestees should do so, but may choose
 * to respond with fewer than the total requested; however, they must not provide more than {@code num} peers. If a
 * requestee receives a request that contains more IDs in {@code requested} than {@code num} it should signal an error
 * to the requestor as this is likely a sign of implementation issues at the requestor. If a requestor receives a
 * response with more peers than the {@code num} in its request, it is free to choose how it acts.
 *
 * @param num The total number of peers you wish to receive information on.
 * @param requested Specific peers you wish to be included in the response.
 */
public record PeersRequest(int num, List<PeerId> requested) {
    public PeersRequest {
        if(num <= 0) {
            throw new IllegalArgumentException("num must be greater than 0");
        }
        if(requested.size() > num) {
            throw new IllegalArgumentException("requested cannot exceed num");
        }
    }
    public PeersRequest(List<PeerId> requested) {
        this(requested.size(), requested);
    }
    public PeersRequest(int num) {
        this(num, new ArrayList<>());
    }
}
