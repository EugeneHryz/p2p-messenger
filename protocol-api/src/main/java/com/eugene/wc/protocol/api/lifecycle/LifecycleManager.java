package com.eugene.wc.protocol.api.lifecycle;

import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseOpenListener;

import java.util.concurrent.ExecutorService;

public interface LifecycleManager {

    enum StartResult {
        ALREADY_RUNNING,
        DB_ERROR,
        SERVICE_ERROR,
        SUCCESS
    }

    enum State {
        STARTING,
        STARTING_SERVICES,
        STARTED,
        STOPPING
    }

    void registerService(Service service);

    void registerForShutdown(ExecutorService executorService);

    void registerDatabaseOpenListener(DatabaseOpenListener l);

    StartResult startServices(SecretKey secretKey);

    void stopServices();

    void waitForDatabase() throws InterruptedException;

    void waitForStartup() throws InterruptedException;

    void waitForShutdown() throws InterruptedException;
}
