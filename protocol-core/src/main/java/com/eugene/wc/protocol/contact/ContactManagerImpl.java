package com.eugene.wc.protocol.contact;

import static com.eugene.wc.protocol.api.transport.TransportConstants.*;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportKeys;
import com.eugene.wc.protocol.api.util.ByteUtils;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ContactManagerImpl implements ContactManager {

    private static final Logger logger = Logger.getLogger(ContactManagerImpl.class.getName());

    private final CryptoComponent crypto;
    private final DatabaseComponent db;
    private final List<ContactHook> hooks = new CopyOnWriteArrayList<>();

    @Inject
    public ContactManagerImpl(DatabaseComponent db, CryptoComponent crypto) {
        this.db = db;
        this.crypto = crypto;
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
            logger.warning("Unable to get all contacts\n" + e);
        }
        return contacts;
    }

    @Override
    public ContactId recogniseContact(Tag tag) {
        ContactId contactId = null;
        try {
            List<TransportKeys> allKeys = db.runInTransactionWithResult(true, db::getAllTransportKeys);

            for (TransportKeys keys : allKeys) {
                byte[] number = new byte[ByteUtils.INT_64_BYTES];
                ByteUtils.writeUint64(0, number, 0);

                SecretKey derivedIncomingKey = crypto.deriveKey(keys.getIncomingKey(), null, number);
                byte[] tagBytes = Arrays.copyOf(crypto.mac(derivedIncomingKey, null,
                        number), TAG_LENGTH);

                if (Arrays.equals(tagBytes, tag.getBytes())) {
                    contactId = keys.getContactId();
                    break;
                }
            }
        } catch (DbException e) {
            logger.warning("Unable to get all transport keys\n" + e);
        }
        return contactId;
    }

    @Override
    public boolean rotateContactKeys(ContactId contactId) {
        boolean rotated = false;
        try {
            Contact contact = db.runInTransactionWithResult(true,
                    (txn) -> db.getContactById(txn, contactId));

            // FIXME: for now we just assume that both device's clocks always have the same date
            // (including the time when we first added each other as contacts)
            LocalDate currentDate = LocalDate.now();
            LocalDate addedContactDate = contact.getAddedDate();

            long periodNumber = ChronoUnit.DAYS.between(currentDate, addedContactDate);
            byte[] number = new byte[ByteUtils.INT_64_BYTES];
            ByteUtils.writeUint64(periodNumber, number, 0);

            TransportKeys keys = db.runInTransactionWithResult(true,
                    (txn) -> db.getContactKeys(txn, contactId));

            SecretKey rotatedOutKey = crypto.deriveKey(keys.getOutgoingKey(), KEY_ROTATION, number);
            SecretKey rotatedInKey = crypto.deriveKey(keys.getIncomingKey(), KEY_ROTATION, number);

            TransportKeys rotatedKeys = new TransportKeys(keys.getKeySetId(), contactId,
                    rotatedOutKey, rotatedInKey);
            db.runInTransaction(false, (txn) -> db.mergeContactKeys(txn, rotatedKeys));
            rotated = true;
            logger.info("Contact keys successfully rotated");
        } catch (DbException e) {
            logger.warning("Unable to rotate keys for contactId: " + contactId + "\n" + e);
        }
        return rotated;
    }
}
