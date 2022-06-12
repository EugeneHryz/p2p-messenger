package com.eugene.wc.protocol.api.db;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
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

public interface DatabaseComponent {
    // high-level methods for data access

    boolean open(SecretKey key) throws DbException;

    void close() throws DbException;

    Connection startTransaction(boolean readOnly) throws DbException;

    void commitTransaction(Connection txn) throws DbException;

    void abortTransaction(Connection txn);

    void createLocalIdentity(Connection txn, LocalIdentity local) throws DbException;

    LocalIdentity getLocalIdentity(Connection txn) throws DbException;

    ContactId createContact(Connection txn, Identity remote, IdentityId localId)
            throws DbException, ContactAlreadyExistsException;

    boolean containsGroup(Connection txn, GroupId g) throws DbException;

    void addGroup(Connection txn, Group g) throws DbException;

    void removeGroup(Connection txn, Group g) throws DbException;

    Contact getContact(Connection txn, IdentityId id) throws DbException;

    Contact getContactById(Connection txn, ContactId contactId) throws DbException;

    List<Contact> getAllContacts(Connection txn) throws DbException;

    Message getMessage(Connection txn, MessageId m) throws DbException;

    Map<MessageId, Metadata> getMessageMetadata(Connection txn, GroupId g) throws DbException;

    Metadata getMessageMetadata(Connection txn, MessageId m) throws DbException;

    Metadata getGroupMetadata(Connection txn, GroupId g) throws DbException;

    void removeMessage(Connection txn, MessageId m) throws DbException;

    void addLocalMessage(Connection transaction, Message m, Metadata meta, boolean shared,
                         boolean temporary) throws DbException;

    void mergeMessageMetadata(Connection txn, MessageId m, Metadata meta) throws DbException;

    void mergeGroupMetadata(Connection txn, GroupId g, Metadata meta) throws DbException;

    void mergeContactKeys(Connection txn, TransportKeys keys) throws DbException;

    TransportKeys getContactKeys(Connection txn, ContactId contactId) throws DbException;

    List<TransportKeys> getAllTransportKeys(Connection txn) throws DbException;


    <E extends Exception> void runInTransaction(boolean readOnly, DbRunnable<E> task)
            throws DbException, E;

    <R, E extends Exception> R runInTransactionWithResult(boolean readOnly, DbCallable<R, E> task)
            throws DbException, E;
}
