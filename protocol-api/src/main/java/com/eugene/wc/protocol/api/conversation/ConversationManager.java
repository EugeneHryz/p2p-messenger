package com.eugene.wc.protocol.api.conversation;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.session.ClientId;
import com.eugene.wc.protocol.api.session.Message;

public interface ConversationManager {

    ClientId CLIENT_ID = new ClientId("com.eugene.wc.protocol.conversation");

    MessageQueue registerConversation(ContactId contactId);

    MessageQueue getOutgoingMessageQueue(ContactId contactId);


    // todo: need to think more about it
    void onMessageRead(ContactId contactId, Message message);
}
