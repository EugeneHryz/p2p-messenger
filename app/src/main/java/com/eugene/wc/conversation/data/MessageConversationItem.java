package com.eugene.wc.conversation.data;

import com.eugene.wc.protocol.api.session.MessageId;

import java.time.LocalDateTime;
import java.util.Objects;

public class MessageConversationItem extends ConversationItem {

    private final String messageText;

    public MessageConversationItem(int layoutRes,
                                   MessageId id,
                                   LocalDateTime receivedOrSentTime,
                                   boolean isIncoming,
                                   String messageText) {

        super(layoutRes, id, receivedOrSentTime, isIncoming);
        this.messageText = messageText;
    }

    public String getMessageText() {
        return messageText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MessageConversationItem that = (MessageConversationItem) o;
        return Objects.equals(messageText, that.messageText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageText);
    }
}
