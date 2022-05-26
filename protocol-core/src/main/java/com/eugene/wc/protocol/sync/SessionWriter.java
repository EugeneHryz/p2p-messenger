package com.eugene.wc.protocol.sync;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.data.WdfWriter;
import com.eugene.wc.protocol.api.identity.IdentityId;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SessionWriter extends Thread {

    private static final Logger logger = Logger.getLogger(SessionReader.class.getName());

    private final WdfWriter dataWriter;
    private IdentityId localId;

    private volatile boolean interrupted;
    private final boolean needToIntroduce;

    public SessionWriter(WdfWriter dataWriter, IdentityId localId) {
        this.dataWriter = dataWriter;
        this.localId = localId;
        needToIntroduce = true;
    }

    public SessionWriter(WdfWriter dataWriter) {
        this.dataWriter = dataWriter;
        needToIntroduce = false;
    }

    @Override
    public void run() {
        try {
            if (needToIntroduce) {
                dataWriter.writeRaw(localId.getBytes());
            }

            while (!interrupted) {

                logger.info("Waiting for 5 secs...");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    logger.warning("Interrupted while waiting\n" + e);
                }

                byte[] testData = new byte[8];

                logger.info("Writing test data");
                dataWriter.writeRaw(testData);
                dataWriter.flush();
            }

            dataWriter.writeNull();

        } catch (IOException e) {
            logger.warning("IO error occurred\n" + e);
        }
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
