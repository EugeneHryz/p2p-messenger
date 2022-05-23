package com.eugene.wc.protocol.api.db;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;

import java.sql.Connection;
import java.util.List;

public interface DatabaseComponent {
    // high-level methods for data access

    boolean open(SecretKey key) throws DbException;

    void close() throws DbException;

    Connection startTransaction(boolean readOnly) throws DbException;

    void commitTransaction(Connection txn) throws DbException;

    void abortTransaction(Connection txn);

    void createIdentity(Connection txn, Identity identity) throws DbException;

    Identity getIdentity(Connection txn) throws DbException;

    boolean createContact(Connection txn, Contact contact) throws DbException,
            ContactAlreadyExistsException;

    List<Contact> getAllContacts(Connection txn) throws DbException;


    <E extends Exception> void runInTransaction(boolean readOnly,
                                                DbRunnable<E> task) throws DbException, E;

    <R, E extends Exception> R runInTransactionWithResult(boolean readOnly,
                                                          DbCallable<R, E> task) throws DbException, E;
}
