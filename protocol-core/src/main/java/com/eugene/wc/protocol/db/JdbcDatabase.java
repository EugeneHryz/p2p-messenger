package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.settings.Settings;

import java.sql.Connection;
import java.util.List;

public interface JdbcDatabase {

    boolean open(SecretKey key) throws DbException;

    void close() throws DbException;

    Connection startTransaction(boolean readOnly) throws DbException;

    void commitTransaction(Connection txn) throws DbException;

    void abortTransaction(Connection txn);


    Settings getSettings(Connection txn, String namespace) throws DbException;

    void mergeSettings(Connection txn, Settings settings, String namespace) throws DbException;

    boolean createIdentity(Connection txn, Identity identity) throws DbException;

    Identity getIdentity(Connection txn) throws DbException;

    // returns generated contact id
    int createContact(Connection txn, Contact contact) throws DbException;

    Contact getContactById(Connection txn, int id) throws DbException;

    boolean containsContact(Connection txn, Contact contact) throws DbException;

    List<Contact> getAllContacts(Connection txn) throws DbException;
}
