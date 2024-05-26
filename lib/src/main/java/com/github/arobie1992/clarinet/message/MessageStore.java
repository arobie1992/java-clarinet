package com.github.arobie1992.clarinet.message;


import java.util.Optional;

public interface MessageStore {
    /**
     * Save the message to the message store.
     * @throws ExistingMessageIdException if a message with the given {@code MessageId} is already present.
     * @param message The message to be saved
     */
    void add(DataMessage message);
    Optional<DataMessage> find(MessageId messageId);
}
