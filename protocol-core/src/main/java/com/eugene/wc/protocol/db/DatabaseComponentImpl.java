package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.DbCallable;
import com.eugene.wc.protocol.api.db.DbRunnable;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;

import java.sql.Connection;
import java.util.List;
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
    public void createIdentity(Connection txn, Identity identity) throws DbException {
        db.createIdentity(txn, identity);
    }

    @Override
    public Identity getIdentity(Connection txn) throws DbException {
        return db.getIdentity(txn);
    }

    @Override
    public boolean createContact(Connection txn, Contact contact) throws DbException,
            ContactAlreadyExistsException {
        boolean alreadyExists = db.containsContact(txn, contact);
        boolean created = false;
        if (!alreadyExists) {
            int generatedId = db.createContact(txn, contact);

            if (generatedId != -1) {
                contact.setId(generatedId);
                created = true;
            }
        } else {
            logger.warning("Contact with given name and public key already exists");
            throw new ContactAlreadyExistsException("Contact with given name and " +
                    "public key already exists");
        }
        return created;
    }

    @Override
    public List<Contact> getAllContacts(Connection txn) throws DbException {
        return db.getAllContacts(txn);
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
