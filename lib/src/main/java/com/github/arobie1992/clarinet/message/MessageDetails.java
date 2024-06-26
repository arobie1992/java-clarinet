package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;

import java.util.Objects;

/**
 * A compressed representation of a message and its data.
 * @param messageId The {@link MessageId} of the represented message.
 * @param messageHash A hash of the {@link DataMessage#messageId()}, {@link DataMessage#data()}, and
 *        {@link DataMessage#senderSignature()} of a message.
 */
public record MessageDetails(MessageId messageId, Bytes messageHash) {
    public MessageDetails {
        Objects.requireNonNull(messageId);
    }
}
