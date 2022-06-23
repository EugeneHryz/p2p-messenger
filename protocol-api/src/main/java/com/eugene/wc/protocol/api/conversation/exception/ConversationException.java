package com.eugene.wc.protocol.api.conversation.exception;

public class ConversationException extends Exception {

    public ConversationException() {
        super();
    }

    public ConversationException(String message) {
        super(message);
    }

    public ConversationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversationException(Throwable cause) {
        super(cause);
    }
}
