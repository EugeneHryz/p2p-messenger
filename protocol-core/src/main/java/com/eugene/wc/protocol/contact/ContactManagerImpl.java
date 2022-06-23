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
            throws DbException, ContactAlreadyExistsException {
        ContactId id = db.createContact(txn, remote, localId);
        if (id != null) {
            Contact contact = db.getContactById(txn, id);
            for (ContactHook hook : hooks) hook.addingContact(txn, contact);
        }
        return id;
    }

    @Override
    public Contact getContact(IdentityId identityId) throws DbException {
        Contact c = db.runInTransactionWithResult(true,
                (txn) -> db.getContact(txn, identityId));
        return c;
    }

    @Override
    public Contact getContactById(ContactId contactId) throws DbException {
        Contact c = db.runInTransactionWithResult(true,
                (txn) -> db.getContactById(txn, contactId));
        return c;
    }

    @Override
    public List<Contact> getAllContacts() throws DbException {
        List<Contact> contacts = db.runInTransactionWithResult(true, db::getAllContacts);
        return contacts;
    }

    @Override
    public ContactId recogniseContact(Tag tag) throws DbException {
        ContactId contactId = null;
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
        return contactId;
    }

    @Override
    public void rotateContactKeys(ContactId contactId) throws DbException {
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

        logger.info("Contact keys successfully rotated");
    }
}
