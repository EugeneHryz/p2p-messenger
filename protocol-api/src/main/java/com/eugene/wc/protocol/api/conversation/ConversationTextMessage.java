package com.eugene.wc.protocol.api.conversation;

import com.eugene.wc.protocol.api.session.MessageId;

import java.time.LocalDateTime;
import java.util.Objects;

public class ConversationTextMessage {

    private MessageId messageId;

    private boolean isIncoming;
    private final String text;
    private LocalDateTime sendOrReceivedTime;

    public ConversationTextMessage(boolean isIncoming,
                                   String text,
                                   LocalDateTime sendOrReceivedTime) {
        this.isIncoming = isIncoming;
        this.text = text;
        this.sendOrReceivedTime = sendOrReceivedTime;
    }

    public ConversationTextMessage(String text) {
        this.text = text;
    }

    public MessageId getId() {
        return messageId;
    }

    public void setMessageId(MessageId messageId) {
        this.messageId = messageId;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getTime() {
        return sendOrReceivedTime;
    }

    public void setTime(LocalDateTime time) {
        sendOrReceivedTime = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationTextMessage that = (ConversationTextMessage) o;
        return isIncoming == that.isIncoming
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(text, that.text)
                && Objects.equals(sendOrReceivedTime, that.sendOrReceivedTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, isIncoming, text, sendOrReceivedTime);
    }
}
