package com.eugene.wc.protocol.contact;

import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ContactManagerImpl implements ContactManager {

    private static final Logger logger = Logger.getLogger(ContactManagerImpl.class.getName());

    private final DatabaseComponent db;
    private final List<ContactHook> hooks = new CopyOnWriteArrayList<>();

    @Inject
    public ContactManagerImpl(DatabaseComponent db) {
        this.db = db;
    }

    @Override
    public void registerContactHook(ContactHook hook) {
        hooks.add(hook);
    }

    @Override
    public ContactId createContact(Connection txn, Identity remote, IdentityId localId)
            throws ContactAlreadyExistsException {
        ContactId id = null;
        try {
            id = db.createContact(txn, remote, localId);
            if (id != null) {
                Contact contact = db.getContactById(txn, id);

                logger.info("About to notify all hooks");
                for (ContactHook hook : hooks) hook.addingContact(txn, contact);
            }
        } catch (DbException e) {
            logger.warning("Unable to create contact\n" + e);
        }
        return id;
    }

    @Override
    public Contact getContact(IdentityId identityId) {
        Contact c = null;
        try {
            c = db.runInTransactionWithResult(true, (txn) -> db.getContact(txn, identityId));
        } catch (DbException e) {
            logger.warning("Unable to get contact by id\n" + e);
        }
        return c;
    }

    @Override
    public Contact getContactById(ContactId contactId) {
        Contact c = null;
        try {
            c = db.runInTransactionWithResult(true, (txn) -> db.getContactById(txn, contactId));
        } catch (DbException e) {
            logger.warning("Unable to get contact by id\n" + e);
        }
        return c;
    }

    @Override
    public List<Contact> getAllContacts() {
        List<Contact> contacts = null;
        try {
            contacts = db.runInTransactionWithResult(true, db::getAllContacts);

        } catch (DbException e) {
            logger.warning("Unable to get all contacts " + e);
        }
        return contacts;
    }
}
