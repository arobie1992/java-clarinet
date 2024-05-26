package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.message.DataMessage;
import com.github.arobie1992.clarinet.message.ExistingMessageIdException;
import com.github.arobie1992.clarinet.message.MessageId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMessageStoreTest {

    private final DataMessage message = new DataMessage(new MessageId(ConnectionId.random(), 0), "");
    private InMemoryMessageStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryMessageStore();
    }

    @Test
    void testAdd() {
        store.add(message);
        var messageOpt = store.find(message.messageId());
        assertTrue(messageOpt.isPresent());
        assertEquals(message, messageOpt.get());
    }

    @Test
    void testAddAlreadyExisting() {
        store.add(message);
        var ex = assertThrows(ExistingMessageIdException.class, () -> store.add(message));
        assertEquals(message.messageId(), ex.messageId());
    }

}