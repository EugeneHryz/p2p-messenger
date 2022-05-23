package com.eugene.wc.protocol.contact;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ContactManagerImpl implements ContactManager {

    private static final Logger logger = Logger.getLogger(ContactManagerImpl.class.getName());

    private final DatabaseComponent db;

    @Inject
    public ContactManagerImpl(DatabaseComponent db) {
        this.db = db;
    }

    @Override
    public boolean createContact(Contact contact) throws ContactAlreadyExistsException {
        boolean created = false;
        try {
            created = db.runInTransactionWithResult(false,
                    (txn) -> db.createContact(txn, contact));

        } catch (DbException e) {
            logger.warning("Error while saving contact " + e);
        }
        return created;
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
