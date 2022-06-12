package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.conversation.MessageQueue;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.logging.Logger;

import javax.inject.Inject;

public class RandomMessageGenerator extends Thread {

    private static final Logger logger = Logger.getLogger(RandomMessageGenerator.class.getName());

    private final String[] messages = new String[]{
            "hello!!!",
            "how are you",
            "the weather is nice",
            "i'm tired",
            "i'm happy",
            "i'm sad",
            "today is summer",
            "my name is Zheny",
            "Nice name"
    };

    @Inject
    MessageFactory messageFactory;

    private final Group group;
    private final MessageQueue messageQueue;

    private volatile boolean interrupted = false;

    private final Random random = new Random();

    public RandomMessageGenerator(Group group, MessageQueue messageQueue, ProtocolComponent component) {
        this.group = group;
        this.messageQueue = messageQueue;

        component.inject(this);
    }

    @Override
    public void run() {

        try {
            while (!interrupted) {
                long waitTime = (long) (random.nextDouble() * 10000);
                Thread.sleep(waitTime);

                int arrayIndex = random.nextInt(messages.length);
                String messageContent = messages[arrayIndex];
                long now = System.currentTimeMillis();
                Message message = messageFactory.createMessage(group.getId(), now,
                        messageContent.getBytes(StandardCharsets.UTF_8));
                messageQueue.addMessage(message);
            }
        } catch (InterruptedException e) {
            logger.warning("Interrupted while waiting\n" + e);
        }
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
