package com.eugene.wc.protocol.api.conversation;

import com.eugene.wc.protocol.api.session.Message;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageQueue {

    private final Lock lock = new ReentrantLock();
    private final Queue<Message> messageQueue = new LinkedList<>();

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
}
