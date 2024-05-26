package com.github.arobie1992.clarinet.message;

public class ExistingMessageIdException extends RuntimeException {
    private final MessageId messageId;

    public ExistingMessageIdException(MessageId messageId) {
        super("Message id " + messageId + " already exists");
        this.messageId = messageId;
    }

    public MessageId messageId() {
        return messageId;
    }
}
