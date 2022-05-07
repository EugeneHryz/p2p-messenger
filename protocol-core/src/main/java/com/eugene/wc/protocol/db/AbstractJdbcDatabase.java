package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.settings.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public abstract class AbstractJdbcDatabase implements JdbcDatabase {

    protected static final Logger logger = Logger.getLogger(JdbcDatabase.class.getName());

    // sql code for creating tables
    private static final String CREATE_SETTINGS_TABLE =
            "CREATE TABLE settings"
                    + " (namespace _STRING NOT NULL,"
                    + " key _STRING NOT NULL,"
                    + " value _STRING NOT NULL,"
                    + " PRIMARY KEY (namespace, key))";

    private final DatabaseTypes dbTypes;

    private final Lock connectionsLock = new ReentrantLock();

    private final Queue<Connection> connections = new LinkedList<>();
    private final Condition connectionsAdded = connectionsLock.newCondition();
    private int openConnections = 0;

    public AbstractJdbcDatabase(DatabaseTypes dbTypes) {
        this.dbTypes = dbTypes;
    }

    // is intended to use internally
    protected abstract Connection createConnection() throws SQLException;

    protected void open(String driverName, boolean reopen) throws DbException {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            logger.severe("Unable to load database driver: " + driverName);
            throw new RuntimeException("Unable to load database driver: " + driverName);
        }

        Connection txn = null;
        try {
            txn = startTransaction();
            if (reopen) {
                // todo: migrate?

            } else {
                // first time opening the database
                createTables(txn);
            }

            commitTransaction(txn);
        } catch (DbException e) {
            logger.warning("Error while opening database " + e.getMessage());
            abortTransaction(txn);
            throw e;
        }
    }

    private void createTables(Connection txn) {
        try (Statement statement = txn.createStatement()) {
            // creating other tables
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_SETTINGS_TABLE));
        } catch (SQLException e) {
            logger.warning("Unable to create database tables " + e);
        }
    }

    protected void closeAllConnections() throws SQLException {
        connectionsLock.lock();
        try {
            for (Connection conn : connections) {
                conn.close();
            }
            openConnections -= connections.size();
            connections.clear();
            while (openConnections > 0) {
                while (connections.size() == 0) {
                    try {
                        connectionsAdded.await();
                    } catch (InterruptedException e) {
                        logger.warning("Interrupted while waiting for a connection to be returned");
                    }
                }
                for (Connection conn : connections) {
                    conn.close();
                }
                openConnections -= connections.size();
                connections.clear();
            }
        } finally {
            connectionsLock.unlock();
        }
    }

    @Override
    public Connection startTransaction() throws DbException {
        Connection txn;
        connectionsLock.lock();
        try {
            txn = connections.poll();
        } finally {
            connectionsLock.unlock();
        }

        if (txn == null) {
            try {
                txn = createConnection();
                txn.setAutoCommit(false);

                connectionsLock.lock();
                try {
                    openConnections++;
                } finally {
                    connectionsLock.unlock();
                }
            } catch (SQLException e) {
                logger.warning("Error while starting transaction " + e.getMessage());
                throw new DbException(e);
            }
        }
        return txn;
    }

    @Override
    public void commitTransaction(Connection txn) throws DbException {
        try {
            txn.commit();
            // returning connection to the pool
            connectionsLock.lock();
            try {
                connections.add(txn);
                connectionsAdded.signalAll();
            } finally {
                connectionsLock.unlock();
            }

        } catch (SQLException e) {
            logger.warning("Unable to commit transaction");
            throw new DbException("Unable to commit transaction", e);
        }
    }

    @Override
    public void abortTransaction(Connection txn) {
        try {
            txn.rollback();

            connectionsLock.lock();
            try {
                connections.add(txn);
                connectionsAdded.signalAll();
            } finally {
                connectionsLock.unlock();
            }
        } catch (SQLException e) {
            logger.warning("Unable to rollback transaction");
            try {
                txn.close();
                connectionsLock.lock();
                try {
                    openConnections--;
                } finally {
                    connectionsLock.unlock();
                }
            } catch (SQLException e1) {
                logger.warning("Unable to close connection");
            }
        }
    }

    @Override
    public Settings getSettings(Connection txn, String namespace) throws DbException {
        return null;
    }

    @Override
    public void mergeSettings(Connection txn, Settings settings, String namespace) throws DbException {

    }
}
