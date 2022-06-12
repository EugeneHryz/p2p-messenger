package com.eugene.wc.protocol.db;

import static com.eugene.wc.protocol.api.session.Metadata.REMOVE;
import static com.eugene.wc.protocol.api.session.SyncConstants.MESSAGE_HEADER_LENGTH;
import static com.eugene.wc.protocol.api.session.validation.MessageState.DELIVERED;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.db.exception.MessageDeletedException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.identity.LocalIdentity;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.session.MessageId;
import com.eugene.wc.protocol.api.session.Metadata;
import com.eugene.wc.protocol.api.session.validation.MessageState;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.annotation.Nullable;

public abstract class AbstractJdbcDatabase implements JdbcDatabase {

    protected static final Logger logger = Logger.getLogger(JdbcDatabase.class.getName());

    private static final String CREATE_SETTINGS_TABLE =
            "CREATE TABLE settings"
                    + " (namespace _STRING NOT NULL,"
                    + " key _STRING NOT NULL,"
                    + " value _STRING NOT NULL,"
                    + " PRIMARY KEY (namespace, key))";

    private static final String CREATE_IDENTITIES_TABLE =
            "CREATE TABLE identities (id _HASH NOT NULL," +
                    " name _STRING NOT NULL," +
                    " public_key _BINARY NOT NULL," +
                    " private_key _BINARY NOT NULL," +
                    " PRIMARY KEY (id))";

    private static final String CREATE_CONTACTS_TABLE =
            "CREATE TABLE contacts (id _COUNTER," +
                    " identity_id _HASH NOT NULL," +
                    " local_identity_id _HASH NOT NULL," +
                    " name _STRING NOT NULL," +
                    " alias _STRING," +
                    " added_date DATE NOT NULL," +
                    " public_key _BINARY NOT NULL," +
                    " PRIMARY KEY (id)," +
                    " FOREIGN KEY (local_identity_id)" +
                    " REFERENCES identities (id))";

    private static final String CREATE_GROUPS_TABLE =
            "CREATE TABLE groups"
                    + " (id _HASH NOT NULL,"
                    + " client_id _STRING NOT NULL,"
                    + " descriptor _BINARY NOT NULL,"
                    + " PRIMARY KEY (id))";

    private static final String CREATE_GROUP_METADATA_TABLE =
            "CREATE TABLE group_metadata"
                    + " (group_id _HASH NOT NULL,"
                    + " meta_key _STRING NOT NULL,"
                    + " value _BINARY NOT NULL,"
                    + " PRIMARY KEY (group_id, meta_key),"
                    + " FOREIGN KEY (group_id)"
                    + " REFERENCES groups (id)"
                    + " ON DELETE CASCADE)";

    private static final String CREATE_MESSAGES_TABLE =
            "CREATE TABLE messages"
                    + " (id _HASH NOT NULL,"
                    + " group_id _HASH NOT NULL,"
                    + " timestamp BIGINT NOT NULL,"
                    + " state INT NOT NULL,"
                    + " shared BOOLEAN NOT NULL,"
                    + " temporary BOOLEAN NOT NULL,"
                    // Null if no timer duration has been set
                    + " cleanup_timer_duration BIGINT,"
                    // Null if no timer duration has been set or the timer
                    // hasn't started
                    + " cleanup_deadline BIGINT,"
                    + " length INT NOT NULL,"
                    + " raw BLOB," // Null if message has been deleted
                    + " PRIMARY KEY (id),"
                    + " FOREIGN KEY (group_id)"
                    + " REFERENCES groups (id)"
                    + " ON DELETE CASCADE)";

    private static final String CREATE_MESSAGE_METADATA_TABLE =
            "CREATE TABLE message_metadata"
                    + " (message_id _HASH NOT NULL,"
                    + " group_id _HASH NOT NULL,"
                    + " state INT NOT NULL,"
                    + " meta_key _STRING NOT NULL,"
                    + " value _BINARY NOT NULL,"
                    + " PRIMARY KEY (message_id, meta_key),"
                    + " FOREIGN KEY (message_id)"
                    + " REFERENCES messages (id)"
                    + " ON DELETE CASCADE,"
                    + " FOREIGN KEY (group_id)"
                    + " REFERENCES groups (id)"
                    + " ON DELETE CASCADE)";

    private static final String CREATE_KEYS_TABLE =
            "CREATE TABLE keys" +
                    " (key_set_id _COUNTER," +
                    " contact_id INT NOT NULL," +
                    " outgoing_key _SECRET NOT NULL," +
                    " incoming_key _SECRET NOT NULL," +
                    " PRIMARY KEY (key_set_id)," +
                    " FOREIGN KEY (contact_id)" +
                    " REFERENCES contacts (id)" +
                    " ON DELETE CASCADE)";

    private final DatabaseTypes dbTypes;
    private final MessageFactory messageFactory;

    private final Lock connectionsLock = new ReentrantLock();

    private final Queue<Connection> connections = new LinkedList<>();
    private final Condition connectionsAdded = connectionsLock.newCondition();
    private int openConnections = 0;

    public AbstractJdbcDatabase(DatabaseTypes dbTypes, MessageFactory messageFactory) {
        this.dbTypes = dbTypes;
        this.messageFactory = messageFactory;
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
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_GROUPS_TABLE));
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_GROUP_METADATA_TABLE));
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_MESSAGES_TABLE));
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_MESSAGE_METADATA_TABLE));
            statement.executeUpdate(dbTypes.replaceTypes(CREATE_KEYS_TABLE));
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
                txn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
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
    public boolean createLocalIdentity(Connection txn, LocalIdentity local) throws DbException {
        String sql = "INSERT INTO identities" +
                " (id, name, public_key, private_key)" +
                " VALUES (?, ?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, local.getId().getBytes());
            prStatement.setString(2, local.getName());
            prStatement.setBytes(3, local.getPublicKey().getBytes());
            prStatement.setBytes(4, local.getPrivateKey().getBytes());

            return prStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.warning("Unable to create local identity\n" + e);
            throw new DbException("Unable to create local identity", e);
        }
    }

    @Override
    public List<LocalIdentity> getAllLocalIdentities(Connection txn) throws DbException {
        String sql = "SELECT id, name, public_key, private_key FROM identities";

        try (Statement statement = txn.createStatement()) {

            ResultSet rs = statement.executeQuery(sql);
            List<LocalIdentity> localIdentities = new ArrayList<>();
            while (rs.next()) {
                IdentityId id = new IdentityId(rs.getBytes(1));
                String name = rs.getString(2);
                PublicKey pubKey = new PublicKey(rs.getBytes(3));
                PrivateKey prKey = new PrivateKey(rs.getBytes(4));

                localIdentities.add(new LocalIdentity(id, pubKey, name, prKey));
            }

            return localIdentities;
        } catch (SQLException e) {
            logger.warning("Unable to get all local identities\n" + e);
            throw new DbException("Unable to get all local identities", e);
        }
    }

    @Override
    public ContactId createContact(Connection txn, Identity remote, IdentityId localId) throws DbException {
        String sql = "INSERT INTO contacts" +
                " (local_identity_id, identity_id, name, added_date, public_key)" +
                " VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            prStatement.setBytes(1, localId.getBytes());
            prStatement.setBytes(2, remote.getId().getBytes());
            prStatement.setString(3, remote.getName());

            Date localDate = Date.valueOf(LocalDate.now().toString());
            prStatement.setDate(4, localDate);
            prStatement.setBytes(5, remote.getPublicKey().getBytes());

            int generatedId = -1;
            if (prStatement.executeUpdate() > 0) {
                ResultSet resultSet = prStatement.getGeneratedKeys();
                if (resultSet.next()) {
                    generatedId = resultSet.getInt(1);
                }
            }
            return new ContactId(generatedId);

        } catch (SQLException e) {
            logger.warning("Unable to create contact\n" + e);
            throw new DbException("Unable to create contact", e);
        }
    }

    @Override
    public boolean containsContact(Connection txn, IdentityId remote, IdentityId local) throws DbException {
        String sql = "SELECT NULL FROM contacts" +
                " WHERE identity_id = ? AND local_identity_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, remote.getBytes());
            prStatement.setBytes(2, local.getBytes());

            ResultSet resultSet = prStatement.executeQuery();
            return resultSet.next();

        } catch (SQLException e) {
            logger.warning("Unable to check if contact exists\n" + e);
            throw new DbException("Unable to check if contact exists", e);
        }
    }

    @Override
    public List<Contact> getAllContacts(Connection txn) throws DbException {
        String sql = "SELECT id, local_identity_id, identity_id, name, added_date, public_key" +
                " FROM contacts";

        try (Statement statement = txn.createStatement()) {

            ResultSet rs = statement.executeQuery(sql);
            List<Contact> contacts = new ArrayList<>();
            while (rs.next()) {
                ContactId contactId = new ContactId(rs.getInt(1));
                IdentityId localIdentityId = new IdentityId(rs.getBytes(2));
                IdentityId identityId = new IdentityId(rs.getBytes(3));
                String name = rs.getString(4);
                LocalDate addedDate = LocalDate.parse(rs.getDate(5).toString());
                PublicKey pubKey = new PublicKey(rs.getBytes(6));

                Identity identity = new Identity(identityId, pubKey, name);
                contacts.add(new Contact(contactId, localIdentityId, identity, addedDate));
            }
            return contacts;

        } catch (SQLException e) {
            logger.warning("Unable to get all contacts\n" + e);
            throw new DbException("Unable to get all contacts", e);
        }
    }

    @Override
    public boolean containsMessage(Connection txn, MessageId m) throws DbException {
        String sql = "SELECT NULL FROM messages WHERE id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, m.getBytes());

            ResultSet rs = prStatement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.warning("Unable to check if message exists\n" + e);
            throw new DbException("Unable to check if message exists", e);
        }
    }

    @Override
    public Message getMessage(Connection txn, MessageId m) throws DbException {
        String sql = "SELECT group_id, timestamp, raw FROM messages"
                + " WHERE id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, m.getBytes());

            ResultSet rs = prStatement.executeQuery();

            if (rs.next()) {
                GroupId groupId = new GroupId(rs.getBytes(1));
                long timestamp = rs.getLong(2);
                byte[] raw = rs.getBytes(3);

                if (raw == null) throw new MessageDeletedException();
                if (raw.length <= MESSAGE_HEADER_LENGTH) throw new AssertionError();
                byte[] body = new byte[raw.length - MESSAGE_HEADER_LENGTH];
                System.arraycopy(raw, MESSAGE_HEADER_LENGTH, body, 0, body.length);

                return new Message(m, groupId, timestamp, body);
            }
        } catch (SQLException e) {
            logger.warning("Unable to get message\n" + e);
            throw new DbException("Unable to get message", e);
        }
        return null;
    }

    @Override
    public void addMessage(Connection txn, Message m, MessageState state, boolean shared,
                           boolean temporary, @Nullable ContactId sender) throws DbException {
        String sql = "INSERT INTO messages (id, group_id, timestamp,"
                + " state, shared, temporary, length, raw)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {

            prStatement.setBytes(1, m.getId().getBytes());
            prStatement.setBytes(2, m.getGroupId().getBytes());
            prStatement.setLong(3, m.getTimestamp());
            prStatement.setInt(4, state.getValue());
            prStatement.setBoolean(5, shared);
            prStatement.setBoolean(6, temporary);

            byte[] raw = messageFactory.getRawMessage(m);
            prStatement.setInt(7, raw.length);
            prStatement.setBytes(8, raw);
            int affected = prStatement.executeUpdate();

        } catch (SQLException e) {
            logger.warning("Unable to create a message\n" + e);
            throw new DbException("Unable to create a message", e);
        }
    }

    @Override
    public void mergeMessageMetadata(Connection txn, MessageId m,
                                     Metadata meta) throws DbException {
        PreparedStatement ps;
        ResultSet rs;
        try {
            Map<String, byte[]> added = removeOrUpdateMetadata(txn,
                    m.getBytes(), meta, "message_metadata", "message_id");
            if (added.isEmpty()) return;

            String sql = "SELECT group_id, state FROM messages"
                    + " WHERE id = ?";
            ps = txn.prepareStatement(sql);
            ps.setBytes(1, m.getBytes());
            rs = ps.executeQuery();
            if (!rs.next()) throw new DbException();
            GroupId g = new GroupId(rs.getBytes(1));
            MessageState state = MessageState.fromValue(rs.getInt(2));
            rs.close();
            ps.close();

            sql = "INSERT INTO message_metadata"
                    + " (message_id, group_id, state, meta_key, value)"
                    + " VALUES (?, ?, ?, ?, ?)";
            ps = txn.prepareStatement(sql);
            ps.setBytes(1, m.getBytes());
            ps.setBytes(2, g.getBytes());
            ps.setInt(3, state.getValue());
            for (Map.Entry<String, byte[]> e : added.entrySet()) {
                ps.setString(4, e.getKey());
                ps.setBytes(5, e.getValue());
                ps.addBatch();
            }
            int[] batchAffected = ps.executeBatch();
            if (batchAffected.length != added.size())
                throw new DbException();
            for (int rows : batchAffected)
                if (rows != 1) throw new DbException();
            ps.close();
        } catch (SQLException e) {
            logger.warning("Unable to merge message metadata\n" + e);
            throw new DbException("Unable to merge message metadata", e);
        }
    }

    @Override
    public void mergeGroupMetadata(Connection txn, GroupId g, Metadata meta) throws DbException {
        String sql = "INSERT INTO group_metadata (group_id, meta_key, value)"
                + " VALUES (?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {

            Map<String, byte[]> added = removeOrUpdateMetadata(txn, g.getBytes(), meta,
                    "group_metadata", "group_id");
            if (added.isEmpty()) return;

            prStatement.setBytes(1, g.getBytes());
            for (Map.Entry<String, byte[]> e : added.entrySet()) {
                prStatement.setString(2, e.getKey());
                prStatement.setBytes(3, e.getValue());
                prStatement.addBatch();
            }
            int[] batchAffected = prStatement.executeBatch();

        } catch (SQLException e) {
            logger.warning("Unable to merge group metadata\n" + e);
            throw new DbException(e);
        }
    }

    @Override
    public boolean containsGroup(Connection txn, GroupId g) throws DbException {
        String sql = "SELECT NULL FROM groups WHERE id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, g.getBytes());

            ResultSet rs = prStatement.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.warning("Unable to check if group exists\n" + e);
            throw new DbException("Unable to check if group exists", e);
        }
    }

    @Override
    public void addGroup(Connection txn, Group g) throws DbException {
        String sql = "INSERT INTO groups"
                + " (id, client_id, descriptor)"
                + " VALUES (?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, g.getId().getBytes());
            prStatement.setString(2, g.getClientId().getString());
            prStatement.setBytes(3, g.getDescriptor());

            prStatement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Unable to add new group\n" + e);
            throw new DbException("Unable to add new group", e);
        }
    }

    @Override
    public void removeGroup(Connection txn, GroupId g) throws DbException {
        String sql = "DELETE FROM groups WHERE group_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, g.getBytes());

            prStatement.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Unable to remove group with a given id\n" + e);
            throw new DbException("Unable to remove group with a given id", e);
        }
    }

    @Override
    public Contact getContact(Connection txn, IdentityId id) throws DbException {
        String sql = "SELECT id, identity_id, local_identity_id, name, added_date, public_key"
                + " FROM contacts"
                + " WHERE identity_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, id.getBytes());

            ResultSet rs = prStatement.executeQuery();

            Contact contact = null;
            if (rs.next()) {
                ContactId contactId = new ContactId(rs.getInt(1));
                IdentityId identityId = new IdentityId(rs.getBytes(2));
                IdentityId localIdentityId = new IdentityId(rs.getBytes(3));
                String name = rs.getString(4);
                LocalDate addedDate = LocalDate.parse(rs.getDate(5).toString());
                PublicKey publicKey = new PublicKey(rs.getBytes(6));

                Identity identity = new Identity(identityId, publicKey, name);
                contact = new Contact(contactId, localIdentityId, identity, addedDate);
            }
            return contact;
        } catch (SQLException e) {
            logger.warning("Unable to get contact by id\n" + e);
            throw new DbException("Unable to get contact by id", e);
        }
    }

    @Override
    public Contact getContactById(Connection txn, ContactId contactId) throws DbException {
        String sql = "SELECT identity_id, local_identity_id, name, added_date, public_key"
                + " FROM contacts"
                + " WHERE id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setInt(1, contactId.getInt());

            ResultSet rs = prStatement.executeQuery();

            Contact contact = null;
            if (rs.next()) {
                IdentityId identityId = new IdentityId(rs.getBytes(1));
                IdentityId localIdentityId = new IdentityId(rs.getBytes(2));
                String name = rs.getString(3);
                LocalDate addedDate = LocalDate.parse(rs.getDate(4).toString());
                PublicKey publicKey = new PublicKey(rs.getBytes(5));

                Identity identity = new Identity(identityId, publicKey, name);
                contact = new Contact(contactId, localIdentityId, identity, addedDate);
            }
            return contact;
        } catch (SQLException e) {
            logger.warning("Unable to get contact by id\n" + e);
            throw new DbException("Unable to get contact by id", e);
        }
    }

    @Override
    public Map<MessageId, Metadata> getMessageMetadata(Connection txn, GroupId g) throws DbException {
        String sql = "SELECT message_id, meta_key, value"
                + " FROM message_metadata"
                + " WHERE group_id = ? AND state = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {

            prStatement.setBytes(1, g.getBytes());
            prStatement.setInt(2, DELIVERED.getValue());

            ResultSet rs = prStatement.executeQuery();
            Map<MessageId, Metadata> all = new HashMap<>();
            while (rs.next()) {
                MessageId messageId = new MessageId(rs.getBytes(1));
                Metadata metadata = all.get(messageId);
                if (metadata == null) {
                    metadata = new Metadata();
                    all.put(messageId, metadata);
                }
                metadata.put(rs.getString(2), rs.getBytes(3));
            }
            return all;
        } catch (SQLException e) {
            logger.warning("Unable to get message metadata from given group " + g + "\n" + e);
            throw new DbException("Unable to get message metadata from given group " + g, e);
        }
    }

    @Override
    public Metadata getMessageMetadata(Connection txn, MessageId m) throws DbException {
        String sql = "SELECT meta_key, value FROM message_metadata"
                + " WHERE state = ? AND message_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setInt(1, DELIVERED.getValue());
            prStatement.setBytes(2, m.getBytes());
            ResultSet rs = prStatement.executeQuery();
            Metadata metadata = new Metadata();
            while (rs.next()) {
                metadata.put(rs.getString(1), rs.getBytes(2));
            }
            return metadata;

        } catch (SQLException e) {
            logger.warning("Unable to get message metadata with given messageId: " + m + "\n" + e);
            throw new DbException("Unable to get message metadata with given messageId: " + m, e);
        }
    }

    @Override
    public void removeMessage(Connection txn, MessageId m) throws DbException {
        String sql = "DELETE FROM messages WHERE id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, m.getBytes());
            int affected = prStatement.executeUpdate();

        } catch (SQLException e) {
            logger.warning("Unable to remove message with give id: " + m + "\n" + e);
            throw new DbException("Unable to remove message with give id: " + m, e);
        }
    }

    @Override
    public Metadata getGroupMetadata(Connection txn, GroupId g) throws DbException {
        String sql = "SELECT meta_key, value FROM group_metadata"
                + " WHERE group_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, g.getBytes());

            ResultSet rs = prStatement.executeQuery();
            Metadata metadata = new Metadata();
            while (rs.next()) {
                metadata.put(rs.getString(1), rs.getBytes(2));
            }
            return metadata;
        } catch (SQLException e) {
            logger.warning("Unable to get group metadata\n" + e);
            throw new DbException("Unable to get group metadata " + e);
        }
    }

    private Map<String, byte[]> removeOrUpdateMetadata(Connection txn, byte[] id, Metadata meta,
                                                       String tableName, String columnName) throws DbException {
        PreparedStatement ps;
        try {
            // Determine which keys are being removed
            List<String> removed = new ArrayList<>();
            Map<String, byte[]> notRemoved = new HashMap<>();
            for (Map.Entry<String, byte[]> e : meta.entrySet()) {
                if (e.getValue() == REMOVE) removed.add(e.getKey());
                else notRemoved.put(e.getKey(), e.getValue());
            }
            // Delete any keys that are being removed
            if (!removed.isEmpty()) {
                String sql = "DELETE FROM " + tableName
                        + " WHERE " + columnName + " = ? AND meta_key = ?";
                ps = txn.prepareStatement(sql);
                ps.setBytes(1, id);
                for (String key : removed) {
                    ps.setString(2, key);
                    ps.addBatch();
                }
                int[] batchAffected = ps.executeBatch();
                if (batchAffected.length != removed.size())
                    throw new DbException();
                for (int rows : batchAffected) {
                    if (rows < 0) throw new DbException();
                    if (rows > 1) throw new DbException();
                }
                ps.close();
            }
            if (notRemoved.isEmpty()) return Collections.emptyMap();
            // Update any keys that already exist
            String sql = "UPDATE " + tableName + " SET value = ?"
                    + " WHERE " + columnName + " = ? AND meta_key = ?";
            ps = txn.prepareStatement(sql);
            ps.setBytes(2, id);
            for (Map.Entry<String, byte[]> e : notRemoved.entrySet()) {
                ps.setBytes(1, e.getValue());
                ps.setString(3, e.getKey());
                ps.addBatch();
            }
            int[] batchAffected = ps.executeBatch();
            if (batchAffected.length != notRemoved.size())
                throw new DbException();
            for (int rows : batchAffected) {
                if (rows < 0) throw new DbException();
                if (rows > 1) throw new DbException();
            }
            ps.close();
            // Are there any keys that don't already exist?
            Map<String, byte[]> added = new HashMap<>();
            int updateIndex = 0;
            for (Map.Entry<String, byte[]> e : notRemoved.entrySet()) {
                if (batchAffected[updateIndex++] == 0)
                    added.put(e.getKey(), e.getValue());
            }
            return added;
        } catch (SQLException e) {
            logger.warning("Unable to remove or update metadata\n" + e);
            throw new DbException("Unable to remove or update metadata", e);
        }
    }

    @Override
    public boolean addKeysForContact(Connection txn, TransportKeys transportKeys) throws DbException {
        String sql = "INSERT INTO keys"
                + " (contact_id, outgoing_key, incoming_key)"
                + " VALUES (?, ?, ?)";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setInt(1, transportKeys.getContactId().getInt());
            prStatement.setBytes(2, transportKeys.getOutgoingKey().getBytes());
            prStatement.setBytes(3, transportKeys.getIncomingKey().getBytes());

            return prStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.info("Unable to add transport keys\n");
            throw new DbException("Unable to add transport keys", e);
        }
    }

    @Override
    public TransportKeys getKeysForContact(Connection txn, ContactId contactId) throws DbException {
        String sql = "SELECT key_set_id, outgoing_key, incoming_key" +
                " FROM keys" +
                " WHERE contact_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setInt(1, contactId.getInt());

            ResultSet rs = prStatement.executeQuery();
            TransportKeys keys = null;
            if (rs.next()) {
                Integer keySetId = rs.getInt(1);
                SecretKey outKey = new SecretKey(rs.getBytes(2));
                SecretKey inKey = new SecretKey(rs.getBytes(3));

                keys = new TransportKeys(keySetId, contactId, outKey, inKey);
            }
            return keys;

        } catch (SQLException e) {
            logger.info("Unable to add transport keys\n");
            throw new DbException("Unable to add transport keys", e);
        }
    }

    @Override
    public List<TransportKeys> getAllTransportKeys(Connection txn) throws DbException {
        String sql = "SELECT key_set_id, contact_id, outgoing_key, incoming_key" +
                " FROM keys";

        try (Statement statement = txn.createStatement()) {
            ResultSet rs = statement.executeQuery(sql);
            List<TransportKeys> keys = new ArrayList<>();
            while (rs.next()) {
                Integer keySetId = rs.getInt(1);
                ContactId contactId = new ContactId(rs.getInt(2));
                SecretKey outKey = new SecretKey(rs.getBytes(3));
                SecretKey inKey = new SecretKey(rs.getBytes(4));

                keys.add(new TransportKeys(keySetId, contactId, outKey, inKey));
            }
            return keys;

        } catch (SQLException e) {
            logger.info("Unable to get all transport keys\n");
            throw new DbException("Unable to get all transport keys", e);
        }
    }

    @Override
    public boolean updateKeysForContact(Connection txn, TransportKeys transportKeys) throws DbException {
        String sql = "UPDATE keys"
                + " SET outgoing_key = ?, incoming_key = ?"
                + " WHERE contact_id = ?";

        try (PreparedStatement prStatement = txn.prepareStatement(sql)) {
            prStatement.setBytes(1, transportKeys.getOutgoingKey().getBytes());
            prStatement.setBytes(2, transportKeys.getIncomingKey().getBytes());
            prStatement.setInt(3, transportKeys.getContactId().getInt());

            return prStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.info("Unable to update transport keys with contact id: "
                    + transportKeys.getContactId() + "\n" + e);
            throw new DbException("Unable to update transport keys", e);
        }
    }
}
