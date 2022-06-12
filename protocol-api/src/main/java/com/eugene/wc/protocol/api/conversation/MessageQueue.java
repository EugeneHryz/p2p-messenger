package com.eugene.wc.protocol.api.conversation;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.session.Message;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// represents messages to be send to a given contact
public class MessageQueue {

    private final Lock lock = new ReentrantLock();
    private final Queue<Message> messageQueue = new LinkedList<>();

    private final ContactId contactId;

    public MessageQueue(ContactId contactId) {
        this.contactId = contactId;
    }

    public void addMessage(Message message) {
        lock.lock();
        try {
            messageQueue.add(message);
        } finally {
            lock.unlock();
        }
    }

    public Message pollMessage() {
        lock.lock();
        try {
            return messageQueue.poll();
        } finally {
            lock.unlock();
        }
    }

    public ContactId getContactId() {
        return contactId;
    }
}
