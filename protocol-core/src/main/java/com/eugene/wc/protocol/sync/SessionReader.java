package com.eugene.wc.protocol.sync;

import com.eugene.wc.protocol.api.data.WdfReader;

import java.io.IOException;
import java.util.logging.Logger;

public class SessionReader extends Thread {

    private static final Logger logger = Logger.getLogger(SessionReader.class.getName());

    private final WdfReader dataReader;

    private volatile boolean interrupted;
    private final boolean needToRecognise;

    private SyncSessionCallback callback;

    public SessionReader(WdfReader dataReader) {
        this.dataReader = dataReader;
        this.needToRecognise = false;
    }

    public SessionReader(WdfReader dataReader, SyncSessionCallback callback) {
        this.dataReader = dataReader;
        this.needToRecognise = true;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            if (needToRecognise) {
                byte[] remoteId = dataReader.readRaw();
                callback.onRemoteIdReceived(remoteId);
            }
            // wait for initial data
            dataReader.readRaw();

            while (!interrupted) {

                if (dataReader.hasNull()) {
                    return;
                }
//                try {
//                    logger.info("Waiting for 5 seconds...");
//                    TimeUnit.SECONDS.sleep(5);
//                } catch (InterruptedException e) {
//                    logger.warning("Interrupted while waiting\n" + e);
//                }
                byte[] rawData = dataReader.readRaw();
//                if (dataReader.available() == 0) {
//                    logger.warning("Haven't received keepalive message, closing connection");
//                    interrupted = true;
//                } else {
//
//                }
            }

        } catch (IOException e) {
            logger.warning("IO error occurred\n" + e);
        }
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
