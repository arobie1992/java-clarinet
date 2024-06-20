package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

/**
 * The data this node has for the queried message.
 * <p>
 * If the node does not have a record of the message, it must return null for all three fields. If the node does have a
 * record of the message, then it must ensure it uses the appropriate fields for itself and the given requestor. If this
 * node was the sender in the connection or the querying node was the sender, this node must use
 * {@link DataMessage#senderParts()} for appropriate operations. Otherwise, it must use
 * {@link DataMessage#witnessParts()}.
 * @param hash The hash of the appropriate parts for the queried message.
 * @param signature The signature of {@code hash}.
 * @param hashAlgorithm The hashing algorithm this node used to generate {@code hash}.
 */
public record QueryResponse(Bytes hash, Bytes signature, String hashAlgorithm) {
}
