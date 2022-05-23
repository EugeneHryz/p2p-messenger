package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.settings.Settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public abstract class AbstractJdbcDatabase implements JdbcDatabase {

    protected static final Logger logger = Logger.getLogger(JdbcDatabase.class.getName());

    private static final String CREATE_SETTINGS_TABLE =
            "CREATE TABLE settings"
                    + " (namespace _STRING NOT NULL,"
                    + " key _STRING NOT NULL,"
                    + " value _STRING NOT NULL,"
                    + " PRIMARY KEY (namespace, key))";

    private static final String CREATE_IDENTITIES_TABLE =
            "CREATE TABLE identities (name _STRING NOT NULL," +
                    " publicKey _BINARY NOT NULL," +
                    " privateKey _BINARY NOT NULL," +
                    " PRIMARY KEY (name))";

    private static final String CREATE_CONTACTS_TABLE =
            "CREATE TABLE contacts (id _COUNTER," +
                    " name _STRING NOT NULL," +
                    " alias _STRING," +
                    " publicKey _BINARY NOT NULL," +
                    " PRIMARY KEY (id))";

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
            txn = startTransaction(false);
            if (!reopen) {
                // first time opening the database
                createTables(txn);
            }

            commitTransaction(txn);
        } catch (DbException e) {
            logger.warning("Error while opening database " + e.getMessage());
            if (txn != null) {
                abortTransaction(txn);
            }
            throw e;
        }
    }

    private void createTables(Connection txn) {
        try (Statement statement = txn.createStatement()) {

            statement.executeUpdate(dbTypes.replaceTypes(CREATE_SETTINGS_TABLE));
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_IDENTITIES_TABLE));
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_CONTACTS_TABLE));
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
    public Connection startTransaction(boolean readOnly) throws DbException {
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
                txn.setAutoCommit(readOnly);

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
            if (!txn.getAutoCommit()) {
                txn.commit();
            }
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
            if (!txn.getAutoCommit()) {
                txn.rollback();
            }

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

    @Override
    public boolean createIdentity(Connection txn, Identity identity) throws DbException {
        String sql = "INSERT INTO identities" +
                " (name, publicKey, privateKey)" +
                " VALUES (?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setString(1, identity.getName());
            prStatement.setBytes(2, identity.getPublicKey().getBytes());
            prStatement.setBytes(3, identity.getPrivateKey().getBytes());

            return prStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("Unable to create identity " + e);
            throw new DbException("Unable to create identity", e);
        }
    }

    @Override
    public Identity getIdentity(Connection txn) throws DbException {
        String sql = "SELECT name, publicKey, privateKey FROM identities";
        try (Statement statement = txn.createStatement()) {

            ResultSet resultSet = statement.executeQuery(sql);
            Identity identity = null;
            if (resultSet.next()) {
                String name = resultSet.getString(1);
                byte[] pubKeyBytes = resultSet.getBytes(2);
                byte[] prKeyBytes = resultSet.getBytes(3);

                identity = new Identity(new PublicKey(pubKeyBytes), new PrivateKey(prKeyBytes), name);
            }
            return identity;

        } catch (SQLException e) {
            logger.warning("Unable to get identity by name " + e);
            throw new DbException("Unable to get identity by name", e);
        }
    }

    @Override
    public int createContact(Connection txn, Contact contact) throws DbException {
        String sql = "INSERT INTO contacts" +
                " (name, alias, publicKey)" +
                " VALUES (?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            prStatement.setString(1, contact.getName());
            prStatement.setString(2, contact.getAlias());
            prStatement.setBytes(3, contact.getPublicKey().getBytes());

            int generatedId = -1;
            if (prStatement.executeUpdate() > 0) {
                ResultSet resultSet = prStatement.getGeneratedKeys();
                if (resultSet.next()) {
                    generatedId = resultSet.getInt(1);
                }
            }
            return generatedId;
        } catch (SQLException e) {
            logger.warning("Unable to create contact " + e);
            throw new DbException("Unable to create contact", e);
        }
    }

    @Override
    public Contact getContactById(Connection txn, int id) throws DbException {
        String sql = "SELECT name, alias, publicKey FROM contacts" +
                " WHERE id = ?";
        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setInt(1, id);

            ResultSet resultSet = prStatement.executeQuery();
            Contact contact = null;
            if (resultSet.next()) {
                String name = resultSet.getString(1);
                String alias = resultSet.getString(2);
                byte[] pubKeyBytes = resultSet.getBytes(3);

                contact = new Contact(name, alias, new PublicKey(pubKeyBytes));
                contact.setId(id);
            }
            return contact;

        } catch (SQLException e) {
            logger.warning("Unable to get contact by id " + e);
            throw new DbException("Unable to get contact by id", e);
        }
    }

    @Override
    public boolean containsContact(Connection txn, Contact contact) throws DbException {
        String sql = "SELECT name, publicKey FROM contacts" +
                " WHERE name = ? AND publicKey = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setString(1, contact.getName());
            prStatement.setBytes(2, contact.getPublicKey().getBytes());

            ResultSet resultSet = prStatement.executeQuery();
            return resultSet.next();

        } catch (SQLException e) {
            logger.warning("Unable to check if contact exists " + e);
            throw new DbException("Unable to check if contact exists", e);
        }
    }

    @Override
    public List<Contact> getAllContacts(Connection txn) throws DbException {
        String sql = "SELECT id, name, alias, publicKey FROM contacts";

        try (Statement statement = txn.createStatement()) {

            ResultSet rs = statement.executeQuery(sql);
            List<Contact> contacts = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                String alias = rs.getString(3);
                byte[] publicKeyBytes = rs.getBytes(4);

                Contact contact = new Contact(name, alias, new PublicKey(publicKeyBytes));
                contact.setId(id);
                contacts.add(contact);
            }
            return contacts;

        } catch (SQLException e) {
            logger.warning("Unable to get all contacts " + e);
            throw new DbException("Unable to get all contacts", e);
        }
    }
}
