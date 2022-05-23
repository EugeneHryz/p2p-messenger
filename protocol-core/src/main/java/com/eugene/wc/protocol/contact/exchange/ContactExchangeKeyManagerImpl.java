package com.eugene.wc.protocol.contact.exchange;

import static com.eugene.wc.protocol.api.transport.TransportConstants.ALICE_KEY;
import static com.eugene.wc.protocol.api.transport.TransportConstants.BOB_KEY;
import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;
import static com.eugene.wc.protocol.api.util.ByteUtils.INT_32_BYTES;
import static com.eugene.wc.protocol.api.util.ByteUtils.writeUint32;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeKeyManager;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.transport.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class ContactExchangeKeyManagerImpl implements ContactExchangeKeyManager {

    private static final Logger logger = Logger.getLogger(ContactExchangeManagerImpl.class.getName());

    private static final int INITIAL_KEY_NUMBER = 5;

    private final CryptoComponent crypto;

    private final Lock lock = new ReentrantLock();

    private final List<Pair<SecretKey, Tag>> incomingKeys = new ArrayList<>();
    private final Queue<Pair<SecretKey, Tag>> outgoingKeys = new LinkedList<>();

    private final boolean isAlice;

    private int counter;

    public ContactExchangeKeyManagerImpl(boolean isAlice, CryptoComponent crypto) {
        this.isAlice = isAlice;
        this.crypto = crypto;
    }

    @Override
    public void generateInitialKeys(SecretKey key) {
        lock.lock();
        try {
            generateKeysAndTags(key, INITIAL_KEY_NUMBER);

            counter += INITIAL_KEY_NUMBER;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Pair<SecretKey, Tag> retrieveNextOutgoingKeyAndTag() {
        logger.info("Retrieving next outgoing key and tag");
        logger.info("Queue size: " + outgoingKeys.size());
        lock.lock();
        try {
            return outgoingKeys.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SecretKey retrieveIncomingKey(Tag tag) {
        logger.info("Retrieving incoming key by tag");
        logger.info("List size: " + incomingKeys.size());
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
            try {
                key = targetPair.getFirst();
                incomingKeys.remove(targetPair);
            } finally {
                lock.unlock();
            }
        }
        return key;
    }

    private void generateKeysAndTags(SecretKey secretKey, int numberOfKeys) {
        int baseNumber = counter;
        for (int i = 0; i < numberOfKeys; i++, baseNumber++) {

            SecretKey incomingKey;
            SecretKey outgoingKey;
            byte[] number = new byte[INT_32_BYTES];
            writeUint32(baseNumber, number, 0);

            if (isAlice) {
                outgoingKey = crypto.deriveKey(secretKey, ALICE_KEY, number);
                incomingKey = crypto.deriveKey(secretKey, BOB_KEY, number);
            } else {
                outgoingKey = crypto.deriveKey(secretKey, BOB_KEY, number);
                incomingKey = crypto.deriveKey(secretKey, ALICE_KEY, number);
            }

            byte[] incomingTagBytes = Arrays.copyOf(crypto.mac(incomingKey, null, number),
                    TAG_LENGTH);
            byte[] outgoingTagBytes = Arrays.copyOf(crypto.mac(outgoingKey, null, number),
                    TAG_LENGTH);
            Tag incomingTag = new Tag(incomingTagBytes);
            Tag outgoingTag = new Tag(outgoingTagBytes);

            incomingKeys.add(new Pair<>(incomingKey, incomingTag));
            outgoingKeys.add(new Pair<>(outgoingKey, outgoingTag));
        }
    }
}
