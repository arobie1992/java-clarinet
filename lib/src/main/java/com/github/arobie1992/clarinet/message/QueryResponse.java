package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

import java.util.Objects;

/**
 * The data this node has for the queried message.
 * <p>
 * If the node does not have a record of the message, it must set {@code messageDetails} to the message ID and null for
 * the hash as well as setting {@code signature} and {@code hashAlgorithm} to null.
 * @param messageDetails The {@link MessageDetails} for the given message.
 * @param signature The signature of {@code messageDetails}.
 * @param hashAlgorithm The hashing algorithm this node used to generate {@code hash}.
 */
public record QueryResponse(MessageDetails messageDetails, Bytes signature, String hashAlgorithm) {
    public QueryResponse {
        Objects.requireNonNull(messageDetails);
    }
}
