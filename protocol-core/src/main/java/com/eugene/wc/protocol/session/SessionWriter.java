package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.api.conversation.MessageQueue;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageWriter;

import java.io.IOException;
import java.util.logging.Logger;

public class SessionWriter extends Thread {

    private static final Logger logger = Logger.getLogger(SessionWriter.class.getName());

    private final MessageWriter writer;
    private final MessageQueue messagesToSend;

    private volatile boolean interrupted;

    public SessionWriter(MessageWriter writer, MessageQueue messageQueue) {
        this.writer = writer;
        messagesToSend = messageQueue;
    }

    @Override
    public void run() {
        try {
            while (!interrupted) {
                Message nextMessage = messagesToSend.pollMessage();
                if (nextMessage != null) {
                    writer.sendMessage(nextMessage);
                }
            }
            writer.sendEndOfSession();

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
