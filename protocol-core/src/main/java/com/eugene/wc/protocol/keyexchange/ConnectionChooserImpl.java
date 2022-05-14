package com.eugene.wc.protocol.keyexchange;

import static java.util.logging.Level.INFO;

import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.keyexchange.ConnectionChooser;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeConnection;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ConnectionChooserImpl implements ConnectionChooser {

    private static final Logger logger = Logger.getLogger(ConnectionChooserImpl.class.getName());

    private final BlockingQueue<KeyExchangeConnection> connections = new LinkedBlockingQueue<>();;
    private final Lock lock = new ReentrantLock();

    private final Executor ioExecutor;

    @Inject
    public ConnectionChooserImpl(@IoExecutor Executor ioExecutor) {
        this.ioExecutor = ioExecutor;
    }

    @Override
    public void submitTask(Callable<KeyExchangeConnection> connectionTask) {
        logger.info("About to submit connectionTask");
        ioExecutor.execute(() -> {
            try {
                KeyExchangeConnection conn = connectionTask.call();
                if (conn != null) {
                    logger.info("Adding connection to the queue");
                    addResult(conn);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public KeyExchangeConnection pollConnection(long timeout) {
        try {
            return connections.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warning("Interrupted while polling connection");
        }
        return null;
    }

    private void addResult(KeyExchangeConnection conn) {
        lock.lock();
        try {
            connections.put(conn);
        } catch (InterruptedException e) {
            logger.warning("Interrupted while trying to add connection to unbounded queue " + e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        logger.info("Closing all unused connections");
//        try {
//            for (KeyExchangeConnection conn : connections) {
//                conn.getConnection().getReader().dispose(false, true);
//                conn.getConnection().getWriter().dispose(false);
//            }
//        } catch (IOException e) {
//            logger.warning("Unable to dispose connection " + e);
//        }

        List<KeyExchangeConnection> unused;
        lock.lock();
        try {
            unused = new ArrayList<>(connections);
            connections.clear();
        } finally {
            lock.unlock();
        }
        for (KeyExchangeConnection c : unused) tryToClose(c.getConnection());
    }

    private void tryToClose(DuplexTransportConnection conn) {
        try {
            conn.getReader().dispose(false, true);
            conn.getWriter().dispose(false);
        } catch (IOException e) {
            if (logger.isLoggable(INFO)) logger.info(e.toString());
        }
    }
}
