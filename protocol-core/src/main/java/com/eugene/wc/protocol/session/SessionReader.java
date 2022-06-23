package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageListener;
import com.eugene.wc.protocol.api.session.MessageReader;

import java.io.IOException;
import java.util.logging.Logger;

public class SessionReader extends Thread {

    private static final Logger logger = Logger.getLogger(SessionReader.class.getName());
    private final MessageReader reader;

    private volatile boolean interrupted;

    private final MessageListener callback;

    public SessionReader(MessageReader reader, MessageListener callback) {
        this.reader = reader;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            while (!interrupted) {
                if (reader.isEndOfSession()) {
                    return;
                }
                if (reader.hasMessage()) {
                    Message readMessage = reader.readNextMessage();
                    callback.onMessageReceived(readMessage);
                }
            }
        } catch (IOException e) {
            logger.warning("IO error occurred\n" + e);
        } catch (DecryptionException e) {
            logger.warning("Unable to decrypt message\n" + e);
        }
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
