package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.ExistingMessageIdException;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.message.MessageStore;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMessageStore implements MessageStore {
    private final ConcurrentHashMap<MessageId, DataMessage> messages = new ConcurrentHashMap<>();

    @Override
    public void add(DataMessage message) {
        messages.compute(message.messageId(), (id, existing) -> {
            if (existing != null) {
                throw new ExistingMessageIdException(message.messageId());
            }
            return message;
        });
    }

    @Override
    public Optional<DataMessage> find(MessageId messageId) {
        return Optional.ofNullable(messages.get(messageId));
    }
}
