package com.eugene.wc.protocol.api.conversation;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.conversation.exception.ConversationException;
import com.eugene.wc.protocol.api.session.MessageListener;

import java.util.List;

public interface ContactConversation extends MessageListener {

    ConversationTextMessage sendTextMessage(String msgText) throws ConversationException;

    List<ConversationTextMessage> getMessageHistory() throws ConversationException;

    ContactId getContactId();

    MessageQueue getMessageQueue();

    void setMessageListener(ConversationMessageListener l);
}
