package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExistingMessageIdExceptionTest {

    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);
    private final ExistingMessageIdException exception = new ExistingMessageIdException(messageId);

    @Test
    void testMessage() {
        assertEquals("Message id " + messageId + " already exists", exception.getMessage());
    }

    @Test
    void testMessageId() {
        assertEquals(messageId, exception.messageId());
    }

}