package com.eugene.wc.protocol.db;

import static com.eugene.wc.protocol.api.session.validation.MessageState.DELIVERED;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.DbCallable;
import com.eugene.wc.protocol.api.db.DbRunnable;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.db.exception.NoSuchGroupException;
import com.eugene.wc.protocol.api.db.exception.NoSuchMessageException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.identity.LocalIdentity;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageId;
import com.eugene.wc.protocol.api.session.Metadata;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

public class DatabaseComponentImpl implements DatabaseComponent {

    private static final Logger logger = Logger.getLogger(DatabaseComponentImpl.class.getName());
    private final JdbcDatabase db;

    @Inject
    public DatabaseComponentImpl(JdbcDatabase db) {
        this.db = db;
    }

    @Override
    public boolean open(SecretKey key) throws DbException {
        return db.open(key);
    }

    @Override
    public void close() throws DbException {
        db.close();
    }

    @Override
    public Connection startTransaction(boolean readOnly) throws DbException {
        return db.startTransaction(readOnly);
    }

    @Override
    public void commitTransaction(Connection txn) throws DbException {
        db.commitTransaction(txn);
    }

    @Override
    public void abortTransaction(Connection txn) {
        db.abortTransaction(txn);
    }

    @Override
    public void createLocalIdentity(Connection txn, LocalIdentity local) throws DbException {
        db.createLocalIdentity(txn, local);
    }

    @Override
    public LocalIdentity getLocalIdentity(Connection txn) throws DbException {
        List<LocalIdentity> localIdentities = db.getAllLocalIdentities(txn);
        if (localIdentities.size() != 1) {
            throw new DbException();
        }
        return localIdentities.get(0);
    }

    @Override
    public boolean containsGroup(Connection txn, GroupId g) throws DbException {
        return db.containsGroup(txn, g);
    }

    @Override
    public void addGroup(Connection txn, Group g) throws DbException {
        if (!db.containsGroup(txn, g.getId())) {
            db.addGroup(txn, g);
        }
    }

    @Override
    public void removeGroup(Connection txn, Group g) throws DbException {
        GroupId id = g.getId();
        if (!db.containsGroup(txn, id))
            throw new NoSuchGroupException();
        db.removeGroup(txn, id);

    }

    @Override
    public Contact getContact(Connection txn, IdentityId id) throws DbException {
        return db.getContact(txn, id);
    }

    @Override
    public Contact getContactById(Connection txn, ContactId contactId) throws DbException {
        return db.getContactById(txn, contactId);
    }

    @Override
    public ContactId createContact(Connection txn, Identity remote, IdentityId localId) throws DbException,
            ContactAlreadyExistsException {
        boolean alreadyExists = db.containsContact(txn, remote.getId(), localId);
        if (!alreadyExists) {
            ContactId generatedId = db.createContact(txn, remote, localId);

            return generatedId;
        } else {
            logger.warning("Contact already exists");
            throw new ContactAlreadyExistsException("Contact already exists");
        }
    }

    @Override
    public List<Contact> getAllContacts(Connection txn) throws DbException {
        return db.getAllContacts(txn);
    }

    @Override
    public Message getMessage(Connection txn, MessageId m) throws DbException {
        if (!db.containsMessage(txn, m))
            throw new NoSuchMessageException();
        return db.getMessage(txn, m);
    }

    @Override
    public void removeMessage(Connection txn, MessageId m)
            throws DbException {
        if (!db.containsMessage(txn, m))
            throw new NoSuchMessageException();
        db.removeMessage(txn, m);
    }

    @Override
    public Map<MessageId, Metadata> getMessageMetadata(Connection txn, GroupId g) throws DbException {
        if (!db.containsGroup(txn, g))
            throw new NoSuchGroupException();
        return db.getMessageMetadata(txn, g);
    }

    @Override
    public Metadata getMessageMetadata(Connection txn, MessageId m) throws DbException {
        if (!db.containsMessage(txn, m))
            throw new NoSuchMessageException();
        return db.getMessageMetadata(txn, m);
    }

    @Override
    public Metadata getGroupMetadata(Connection txn, GroupId g) throws DbException {
        if (!db.containsGroup(txn, g))
            throw new NoSuchGroupException();
        return db.getGroupMetadata(txn, g);
    }

    @Override
    public void addLocalMessage(Connection txn, Message m, Metadata meta, boolean shared,
                                boolean temporary) throws DbException {
        if (!db.containsGroup(txn, m.getGroupId()))
            throw new NoSuchGroupException();
        if (!db.containsMessage(txn, m.getId())) {
            db.addMessage(txn, m, DELIVERED, shared, temporary, null);

        }
        db.mergeMessageMetadata(txn, m.getId(), meta);
    }

    @Override
    public void mergeGroupMetadata(Connection txn, GroupId g, Metadata meta) throws DbException {
        if (!db.containsGroup(txn, g))
            throw new NoSuchGroupException();
        db.mergeGroupMetadata(txn, g, meta);
    }

    @Override
    public void mergeMessageMetadata(Connection txn, MessageId m, Metadata meta) throws DbException {
        if (!db.containsMessage(txn, m))
            throw new NoSuchMessageException();
        db.mergeMessageMetadata(txn, m, meta);
    }

    @Override
    public void mergeContactKeys(Connection txn, TransportKeys keys) throws DbException {
        TransportKeys existingKeys = db.getKeysForContact(txn, keys.getContactId());
        if (existingKeys == null) {
            db.addKeysForContact(txn, keys);
        } else {
            db.updateKeysForContact(txn, keys);
        }
    }

    @Override
    public TransportKeys getContactKeys(Connection txn, ContactId contactId) throws DbException {
        return db.getKeysForContact(txn, contactId);
    }

    @Override
    public List<TransportKeys> getAllTransportKeys(Connection txn) throws DbException {
        return db.getAllTransportKeys(txn);
    }


    @Override
    public <E extends Exception> void runInTransaction(boolean readOnly, DbRunnable<E> task)
            throws DbException, E {
        Connection conn = startTransaction(readOnly);
        try {
            task.run(conn);
            commitTransaction(conn);

        } catch (DbException e) {
            abortTransaction(conn);
            throw e;
        }
    }

    @Override
    public <R, E extends Exception> R runInTransactionWithResult(boolean readOnly, DbCallable<R, E> task)
            throws DbException, E {
        Connection conn = startTransaction(readOnly);
        try {
            R result = task.call(conn);
            commitTransaction(conn);
            return result;

        } catch (DbException e) {
            abortTransaction(conn);
            throw e;
        }
    }
}
