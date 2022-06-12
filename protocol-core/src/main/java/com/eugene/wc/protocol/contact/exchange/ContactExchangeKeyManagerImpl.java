package com.eugene.wc.protocol.contact.exchange;

import static com.eugene.wc.protocol.api.transport.TransportConstants.ALICE_KEY;
import static com.eugene.wc.protocol.api.transport.TransportConstants.BOB_KEY;
import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;
import static com.eugene.wc.protocol.api.util.ByteUtils.INT_32_BYTES;
import static com.eugene.wc.protocol.api.util.ByteUtils.writeUint32;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeKeyManager;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ContactExchangeKeyManagerImpl implements ContactExchangeKeyManager {

    private final CryptoComponent crypto;
    private final DatabaseComponent db;

    private final Lock lock = new ReentrantLock();

    private final List<Pair<SecretKey, Tag>> incomingKeys = new ArrayList<>();
    private final Deque<Pair<SecretKey, Tag>> outgoingKeys = new LinkedList<>();

    private final boolean isAlice;
    private int counter;

    public ContactExchangeKeyManagerImpl(boolean isAlice,
                                         CryptoComponent crypto,
                                         DatabaseComponent db) {
        this.isAlice = isAlice;
        this.crypto = crypto;
        this.db = db;
    }

    @Override
    public void generateInitialKeys(SecretKey key) {
        lock.lock();
        try {
            generateKeysWithTags(outgoingKeys, key, DERIVE_KEY_NUMBER, false);
            generateKeysWithTags(incomingKeys, key, DERIVE_KEY_NUMBER, true);

            counter += DERIVE_KEY_NUMBER;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Pair<SecretKey, Tag> retrieveNextOutgoingKeyAndTag() {
        lock.lock();
        try {
            if (outgoingKeys.size() == 1) {
                generateAdditionalKeysWithTags();
            }

            return outgoingKeys.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SecretKey retrieveIncomingKey(Tag tag) {
        Pair<SecretKey, Tag> targetPair = null;
        for (int i = 0; i < incomingKeys.size() && targetPair == null; i++) {
            Pair<SecretKey, Tag> p = incomingKeys.get(i);
            if (p.getSecond().equals(tag)) {
                targetPair = p;
            }
        }

        SecretKey key = null;
        if (targetPair != null) {
            lock.lock();
            if (incomingKeys.size() == 1) {
                generateAdditionalKeysWithTags();
            }

            try {
                key = targetPair.getFirst();
                incomingKeys.remove(targetPair);
            } finally {
                lock.unlock();
            }
        }
        return key;
    }

    @Override
    public void saveLastGeneratedKeys(Connection txn, ContactId contactId) throws DbException {
        SecretKey lastIncomingKey;
        SecretKey lastOutgoingKey;
        lock.lock();
        try {
            lastIncomingKey = incomingKeys.get(incomingKeys.size() - 1).getFirst();
            lastOutgoingKey = outgoingKeys.getLast().getFirst();
        } finally {
            lock.unlock();
        }

        TransportKeys keys = new TransportKeys(contactId, lastOutgoingKey, lastIncomingKey);
        db.mergeContactKeys(txn, keys);
    }

    private void generateAdditionalKeysWithTags() {
        SecretKey lastOutgoingKey = outgoingKeys.getLast().getFirst();
        SecretKey lastIncomingKey = incomingKeys.get(incomingKeys.size() - 1).getFirst();

        generateKeysWithTags(outgoingKeys, lastOutgoingKey, DERIVE_KEY_NUMBER, false);
        generateKeysWithTags(incomingKeys, lastIncomingKey, DERIVE_KEY_NUMBER, true);
    }

    private void generateKeysWithTags(Collection<Pair<SecretKey, Tag>> keysWithTags,
                                      SecretKey secretKey, int numberOfKeys, boolean incoming) {
        int baseNumber = counter;
        for (int i = 0; i < numberOfKeys; i++, baseNumber++) {
            SecretKey derivedKey;
            byte[] number = new byte[INT_32_BYTES];
            writeUint32(baseNumber, number, 0);

            if (isAlice) {
                derivedKey = incoming ? crypto.deriveKey(secretKey, BOB_KEY, number) :
                        crypto.deriveKey(secretKey, ALICE_KEY, number);
            } else {
                derivedKey = incoming ? crypto.deriveKey(secretKey, ALICE_KEY, number) :
                        crypto.deriveKey(secretKey, BOB_KEY, number);
            }
            byte[] tagBytes = Arrays.copyOf(crypto.mac(derivedKey, null, number), TAG_LENGTH);
            Tag tag = new Tag(tagBytes);
            keysWithTags.add(new Pair<>(derivedKey, tag));
        }
    }
}
