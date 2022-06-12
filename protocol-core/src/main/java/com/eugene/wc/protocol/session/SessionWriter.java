package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.api.conversation.MessageQueue;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageWriter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SessionWriter extends Thread {

    private static final Logger logger = Logger.getLogger(SessionReader.class.getName());

    private final MessageWriter messageWriter;
    private final MessageQueue messagesToSend;

    private volatile boolean interrupted;

    public SessionWriter(MessageWriter messageWriter, MessageQueue messageQueue) {
        this.messageWriter = messageWriter;
        messagesToSend = messageQueue;
    }

    @Override
    public void run() {
        try {
            while (!interrupted) {
                Message nextMessage = messagesToSend.pollMessage();
                if (nextMessage != null) {
                    messageWriter.sendMessage(nextMessage);
                }
            }
            messageWriter.sendEndOfSession();

        } catch (IOException e) {
            logger.warning("IO error occurred\n" + e);
        } catch (CryptoException e) {
            logger.warning("Unable to encrypt message\n" + e);
        }
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
