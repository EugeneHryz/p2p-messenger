package com.eugene.wc.protocol.transport;

import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;
import static com.eugene.wc.protocol.api.util.ByteUtils.INT_64_BYTES;
import static com.eugene.wc.protocol.api.util.ByteUtils.writeUint64;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.transport.KeyManager;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.inject.Inject;

public class TransportKeyManager implements KeyManager {

    private static final Logger logger = Logger.getLogger(TransportKeyManager.class.getName());

    @Inject
    CryptoComponent crypto;
    @Inject
    DatabaseComponent db;

    private final Lock lock = new ReentrantLock();

    private TransportKeys rootKeys;
    private final List<Pair<SecretKey, Tag>> incomingKeys = new ArrayList<>();
    private final Deque<Pair<SecretKey, Tag>> outgoingKeys = new LinkedList<>();

    private long inCounter;
    private long outCounter;

    public TransportKeyManager(ContactId contactId, ProtocolComponent component) {
        component.inject(this);

        try {
            TransportKeys keys = db.runInTransactionWithResult(true, (txn) ->
                    db.getContactKeys(txn, contactId));
            logger.info("Loaded transport keys: " + keys);

            generateInitialKeysWithTags(keys);
        } catch (DbException e) {
            logger.warning("Unable to load TransportKeys for a given contact\n" + e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Pair<SecretKey, Tag> retrieveNextOutgoingKeyAndTag() {
        lock.lock();
        try {
            if (outgoingKeys.size() == 1) {
                generateNextOutgoingKeys();
            }

            return outgoingKeys.poll();
        } catch (DbException e) {
            throw new RuntimeException("Unable to save new set of outgoing keys", e);
        } finally {
            lock.unlock();
            logger.info("outgoingKeys size: " + outgoingKeys.size());
        }
    }

    @Override
    public SecretKey retrieveIncomingKey(Tag tag) {
        lock.lock();
        try {
            if (incomingKeys.size() == 1) {
                generateNextIncomingKeys();
            }

            Pair<SecretKey, Tag> targetPair = null;
            for (int i = 0; i < incomingKeys.size() && targetPair == null; i++) {
                Pair<SecretKey, Tag> p = incomingKeys.get(i);
                if (p.getSecond().equals(tag)) {
                    targetPair = p;
                }
            }

            SecretKey key = null;
            if (targetPair != null) {
                key = targetPair.getFirst();
                incomingKeys.remove(targetPair);
                logger.info("incomingKeys size: " + incomingKeys.size());
            }
            return key;

        } catch (DbException e) {
            throw new RuntimeException("Unable to save new set of incoming keys", e);
        } finally {
            lock.unlock();
        }
    }

    private void generateNextOutgoingKeys() throws DbException {
        lock.lock();
        try {
            generateKeysWithTags(outgoingKeys, rootKeys.getOutgoingKey(),
                    TRANSPORT_DERIVE_KEY_NUMBER, false);
            outCounter += TRANSPORT_DERIVE_KEY_NUMBER;
            saveNewContactKeys(rootKeys);
        } finally {
            lock.unlock();
        }
    }

    private void generateNextIncomingKeys() throws DbException {
        lock.lock();
        try {
            generateKeysWithTags(incomingKeys, rootKeys.getIncomingKey(),
                    TRANSPORT_DERIVE_KEY_NUMBER, true);
            inCounter += TRANSPORT_DERIVE_KEY_NUMBER;
            saveNewContactKeys(rootKeys);
        } finally {
            lock.unlock();
        }
    }

    private void saveNewContactKeys(TransportKeys keys) throws DbException {
        db.runInTransaction(false, (txn) -> db.mergeContactKeys(txn, keys));
    }

    private void generateInitialKeysWithTags(TransportKeys keys) throws DbException {
        lock.lock();
        try {
            generateKeysWithTags(outgoingKeys, keys.getOutgoingKey(),
                    TRANSPORT_DERIVE_KEY_NUMBER, false);
            generateKeysWithTags(incomingKeys, keys.getIncomingKey(),
                    TRANSPORT_DERIVE_KEY_NUMBER, true);

            inCounter += TRANSPORT_DERIVE_KEY_NUMBER;
            outCounter += TRANSPORT_DERIVE_KEY_NUMBER;

            saveNewContactKeys(keys);
            rootKeys = keys;
        } finally {
            lock.unlock();
        }
    }

    private void generateKeysWithTags(Collection<Pair<SecretKey, Tag>> keysWithTags,
                                      SecretKey secretKey, int numberOfKeys, boolean incoming) {
        long baseNumber = incoming ? inCounter : outCounter;
        for (int i = 0; i < numberOfKeys; i++, baseNumber++) {
            SecretKey derivedKey;
            byte[] number = new byte[INT_64_BYTES];
            writeUint64(baseNumber, number, 0);

            derivedKey = crypto.deriveKey(secretKey, null, number);

            byte[] tagBytes = Arrays.copyOf(crypto.mac(derivedKey, null, number), TAG_LENGTH);
            Tag tag = new Tag(tagBytes);
            keysWithTags.add(new Pair<>(derivedKey, tag));
        }
    }
}
