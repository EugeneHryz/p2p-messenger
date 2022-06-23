package com.eugene.wc.conversation.data;

import androidx.annotation.LayoutRes;

import com.eugene.wc.protocol.api.session.MessageId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents abstract conversation item (message, attachments, etc.)
 */
public abstract class ConversationItem {

    @LayoutRes
    private final int layoutRes;

    private final MessageId messageId;
    /**
     * represents date and time we received the item if it's an incoming item
     * or date and time we sent the item if it's an outgoing item
     */
    private final LocalDateTime receivedOrSentTime;
    private final boolean isIncoming;

    public ConversationItem(int layoutRes,
                            MessageId id,
                            LocalDateTime receivedOrSentTime,
                            boolean isIncoming) {
        this.layoutRes = layoutRes;
        messageId = id;
        this.receivedOrSentTime = receivedOrSentTime;
        this.isIncoming = isIncoming;
    }

    public int getLayoutRes() {
        return layoutRes;
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public LocalDateTime getTime() {
        return receivedOrSentTime;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationItem that = (ConversationItem) o;
        return layoutRes == that.layoutRes
                && isIncoming == that.isIncoming
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(receivedOrSentTime, that.receivedOrSentTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layoutRes, messageId, receivedOrSentTime, isIncoming);
    }
}
