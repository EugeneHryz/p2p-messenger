package com.eugene.wc.protocol.conversation;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.conversation.ConversationManager;
import com.eugene.wc.protocol.api.conversation.MessageQueue;
import com.eugene.wc.protocol.api.session.Message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ConversationManagerImpl implements ConversationManager {

    private static final Logger logger = Logger.getLogger(ConversationManagerImpl.class.getName());

    private final Object lock = new Object();
    private final List<MessageQueue> outgoingMessages = new ArrayList<>();

    private final ContactManager contactManager;

    @Inject
    public ConversationManagerImpl(ContactManager contactManager) {
        this.contactManager = contactManager;
    }

    @Override
    public MessageQueue registerConversation(ContactId contactId) {
        MessageQueue queue = new MessageQueue(contactId);
        synchronized (lock) {
            outgoingMessages.add(queue);
        }
        return queue;
    }

    @Override
    public MessageQueue getOutgoingMessageQueue(ContactId contactId) {
        for (MessageQueue queue : outgoingMessages) {
            if (queue.getContactId().equals(contactId)) {
                return queue;
            }
        }
        return null;
    }


    @Override
    public void onMessageRead(ContactId contactId, Message message) {
        Contact contact = contactManager.getContactById(contactId);

        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        logger.info("Received message from " + contact.getIdentity().getName() + ": "
                + messageBody);
    }
}
