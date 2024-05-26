package com.github.arobie1992.clarinet.message;

import com.github.arobie1992.clarinet.core.ConnectionId;

public record MessageId(ConnectionId connectionId, long sequenceNumber) {
}
