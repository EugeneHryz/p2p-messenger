package com.eugene.wc.protocol.api.contact;

import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.transport.Tag;

import java.sql.Connection;
import java.util.List;

public interface ContactManager {

    // fixme: is it good idea to ignore DbExceptions in these methods?

    void registerContactHook(ContactHook hook);

    ContactId createContact(Connection txm, Identity remote, IdentityId localId) throws ContactAlreadyExistsException;

    Contact getContact(IdentityId identityId);

    Contact getContactById(ContactId contactId);

    List<Contact> getAllContacts();

    ContactId recogniseContact(Tag tag);

    boolean rotateContactKeys(ContactId contactId);

    interface ContactHook {

        void addingContact(Connection txn, Contact c) throws DbException;

        void removingContact(Connection txn, Contact c) throws DbException;
    }
}
