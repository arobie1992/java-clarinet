package com.github.arobie1992.clarinet.message;

// We use a String instead of a byte[] to ensure that data is immutable
public record DataMessage(MessageId messageId, String data) {
}
