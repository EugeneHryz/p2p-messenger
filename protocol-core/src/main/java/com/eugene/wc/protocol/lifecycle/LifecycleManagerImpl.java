package com.eugene.wc.protocol.lifecycle;

import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;
import com.eugene.wc.protocol.api.lifecycle.Service;
import com.eugene.wc.protocol.api.lifecycle.event.LifecycleStateEvent;
import com.eugene.wc.protocol.api.lifecycle.exception.ServiceException;
import com.eugene.wc.protocol.db.JdbcDatabase;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.inject.Inject;

public class LifecycleManagerImpl implements LifecycleManager {

    private static final Logger logger = Logger.getLogger(LifecycleManagerImpl.class.getName());

    private final EventBus eventBus;
    private final List<Service> services;
    private final List<ExecutorService> executorServices;
    private final JdbcDatabase databaseComponent;

    private final CountDownLatch dbLatch = new CountDownLatch(1);
    private final CountDownLatch servicesLatch = new CountDownLatch(1);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private volatile State state = State.STARTING;

    @Inject
    public LifecycleManagerImpl(EventBus eventBus, JdbcDatabase databaseComponent) {
        this.eventBus = eventBus;
        this.databaseComponent = databaseComponent;

        services = new CopyOnWriteArrayList<>();
        executorServices = new CopyOnWriteArrayList<>();
    }

    @Override
    public void registerService(Service service) {
        services.add(service);
    }

    @Override
    public void registerForShutdown(ExecutorService executorService) {
        executorServices.add(executorService);
    }

    @Override
    public StartResult startServices(SecretKey secretKey) {

        logger.info("Opening database...");
        eventBus.broadcast(new LifecycleStateEvent(state));

        try {
            databaseComponent.open(secretKey);
        } catch (DbException e) {
            logger.warning("Unable to open the database " + e);
            return StartResult.DB_ERROR;
        }

        dbLatch.countDown();
        state = State.STARTING_SERVICES;
        eventBus.broadcast(new LifecycleStateEvent(state));

        logger.info("Starting services...");
        try {
            for (Service service : services) {
                service.startService();
            }
        } catch (ServiceException e) {
            logger.warning("Unable to start service");
            return StartResult.SERVICE_ERROR;
        }
        servicesLatch.countDown();
        state = State.STARTED;
        eventBus.broadcast(new LifecycleStateEvent(state));

        return StartResult.SUCCESS;
    }

    @Override
    public void stopServices() {
        state = State.STOPPING;
        eventBus.broadcast(new LifecycleStateEvent(state));

        try {
            for (Service service : services) {
                service.stopService();
            }
        } catch (ServiceException e) {
            logger.warning("Unable to stop Service " + e);
        }

        for (ExecutorService es : executorServices) {
            es.shutdownNow();
        }
        try {
            databaseComponent.close();
        } catch (DbException e) {
            logger.warning("Failed to close the database " + e);
        }
        shutdownLatch.countDown();
    }

    @Override
    public void waitForDatabase() throws InterruptedException {
        dbLatch.await();
    }

    @Override
    public void waitForStartup() throws InterruptedException {
        servicesLatch.await();
    }

    @Override
    public void waitForShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
}
