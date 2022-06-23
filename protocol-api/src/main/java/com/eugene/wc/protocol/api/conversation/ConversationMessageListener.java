package com.eugene.wc.protocol.api.conversation;

public interface ConversationMessageListener {

    void messageReceived(ConversationTextMessage msg);

    void failedToReceiveMessage(Exception e);
}
