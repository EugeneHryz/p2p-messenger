package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.identity.LocalIdentity;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageId;
import com.eugene.wc.protocol.api.session.Metadata;
import com.eugene.wc.protocol.api.session.validation.MessageState;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public interface JdbcDatabase {

    boolean open(SecretKey key) throws DbException;

    void close() throws DbException;

    Connection startTransaction(boolean readOnly) throws DbException;

    void commitTransaction(Connection txn) throws DbException;

    void abortTransaction(Connection txn);

    Settings getSettings(Connection txn, String namespace) throws DbException;

    void mergeSettings(Connection txn, Settings settings, String namespace) throws DbException;

    boolean createLocalIdentity(Connection txn, LocalIdentity identity) throws DbException;

    List<LocalIdentity> getAllLocalIdentities(Connection txn) throws DbException;

    // returns generated contact id
    ContactId createContact(Connection txn, Identity remote, IdentityId localId) throws DbException;

    boolean containsContact(Connection txn, IdentityId remote, IdentityId local) throws DbException;

    List<Contact> getAllContacts(Connection txn) throws DbException;

    boolean containsMessage(Connection txn, MessageId m) throws DbException;

    Message getMessage(Connection txn, MessageId m) throws DbException;

    boolean containsGroup(Connection txn, GroupId g) throws DbException;

    void addGroup(Connection txn, Group g) throws DbException;

    void removeGroup(Connection txn, GroupId g) throws DbException;

    Contact getContact(Connection txn, IdentityId identityId) throws DbException;

    Contact getContactById(Connection txn, ContactId contactId) throws DbException;

    Map<MessageId, Metadata> getMessageMetadata(Connection txn, GroupId g) throws DbException;

    Metadata getMessageMetadata(Connection txn, MessageId m) throws DbException;

    void mergeMessageMetadata(Connection txn, MessageId m, Metadata meta) throws DbException;

    void mergeGroupMetadata(Connection txn, GroupId g, Metadata meta) throws DbException;

    Metadata getGroupMetadata(Connection txn, GroupId g) throws DbException;

    void addMessage(Connection txn, Message m, MessageState state, boolean shared,
                    boolean temporary, @Nullable ContactId sender) throws DbException;

    void removeMessage(Connection txn, MessageId m) throws DbException;

    boolean addKeysForContact(Connection txn, TransportKeys transportKeys) throws DbException;

    TransportKeys getKeysForContact(Connection txn, ContactId contactId) throws DbException;

    List<TransportKeys> getAllTransportKeys(Connection txn) throws DbException;

    boolean updateKeysForContact(Connection txn, TransportKeys transportKeys) throws DbException;
}
