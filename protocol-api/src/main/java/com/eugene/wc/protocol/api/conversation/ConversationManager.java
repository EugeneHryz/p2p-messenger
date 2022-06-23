package com.eugene.wc.protocol.api.conversation;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.session.ClientId;
import com.eugene.wc.protocol.api.session.Message;

public interface ConversationManager {

    ClientId CLIENT_ID = new ClientId("com.eugene.wc.protocol.conversation");

    ContactConversation registerConversation(Contact contact);

    ContactConversation getContactConversation(ContactId contactId);

}
