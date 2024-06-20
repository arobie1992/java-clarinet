package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.adt.Bytes;
import com.github.arobie1992.clarinet.core.ConnectionId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageForwardTest {

    private final ConnectionId connectionId = ConnectionId.random();
    private final MessageId messageId = new MessageId(connectionId, 0);
    private final MessageSummary summary = new MessageSummary(
            messageId,
            Bytes.of(new byte[]{99,2,70}),
            "SHA-256",
            Bytes.of(new byte[]{54})
    );

    @Test
    void testNullSummary() {
        assertThrows(NullPointerException.class, () -> new MessageForward(null, Bytes.of(new byte[]{17})));
    }

    @Test
    void testNullSignature() {
        assertThrows(NullPointerException.class, () -> new MessageForward(summary, null));
    }

}