package com.eugene.wc.protocol.contact.exchange;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.event.ContactExchangeFailedEvent;
import com.eugene.wc.protocol.api.contact.event.ContactExchangeFinishedEvent;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeKeyManager;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeManager;
import com.eugene.wc.protocol.api.contact.exchange.IdentityDecoder;
import com.eugene.wc.protocol.api.contact.exchange.IdentityEncoder;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeResult;
import com.eugene.wc.protocol.api.keyexchange.event.KeyExchangeFinishedEvent;
import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportReader;
import com.eugene.wc.protocol.api.transport.TransportWriter;
import com.eugene.wc.protocol.transport.TransportReaderImpl;
import com.eugene.wc.protocol.transport.TransportWriterImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class ContactExchangeManagerImpl extends Thread implements ContactExchangeManager {

    private static final Logger logger = Logger.getLogger(ContactExchangeManagerImpl.class.getName());

    private final IdentityManager identityManager;
    private final ContactExchangeKeyManager keyManager;
    private final CryptoComponent crypto;
    private final EventBus eventBus;
    private final ContactManager contactManager;

    private final SecretKey masterKey;
    private final boolean isAlice;

    private final IdentityEncoder identityEncoder;
    private final IdentityDecoder identityDecoder;

    private TransportReader transportReader;
    private TransportWriter transportWriter;

    public ContactExchangeManagerImpl(KeyExchangeResult result, IdentityManager identityManager,
                                      CryptoComponent crypto, EventBus eventBus,
                                      ContactManager contactManager) {
        this.identityManager = identityManager;
        this.crypto = crypto;
        this.eventBus = eventBus;
        this.contactManager = contactManager;
        masterKey = result.getMasterKey();
        isAlice = result.isAlice();
        identityEncoder = new IdentityEncoderImpl();
        identityDecoder = new IdentityDecoderImpl();

        keyManager = new ContactExchangeKeyManagerImpl(result.isAlice(), crypto);
        keyManager.generateInitialKeys(masterKey);

        try {
            OutputStream out = result.getTransport().getConnection().getWriter().getOutputStream();
            InputStream in = result.getTransport().getConnection().getReader().getInputStream();

            transportWriter = new TransportWriterImpl(out);
            transportReader = new TransportReaderImpl(in);

        } catch (IOException e) {
            logger.severe("Unable to initialize");
            throw new AssertionError(e);
        }
    }

    @Override
    public void startContactExchange() {
        start();
    }

    @Override
    public void run() {
        Identity localIdentity;
        try {
            localIdentity = identityManager.getIdentity();
        } catch (DbException e) {
            logger.warning("Unable to load local identity " + e);
            return;
        }
        if (localIdentity == null) {
            logger.warning("Identity is null");
            return;
        }

        try {
            Identity remoteIdentity;
            if (isAlice) {
                encryptAndSendIdentity(localIdentity);
                logger.info("Alice sent her identity and about to receive Bob's");
                remoteIdentity = receiveAndDecryptIdentity();
            } else {

                remoteIdentity = receiveAndDecryptIdentity();
                logger.info("Bob received Alice's identity and about to send his own");
                encryptAndSendIdentity(localIdentity);
            }
            logger.info("Received remote identity!!! Name: " + remoteIdentity.getName());
            if (storeContact(remoteIdentity)) {
                logger.info("Contact created in the db");
                eventBus.broadcast(new ContactExchangeFinishedEvent());
            } else {
                logger.info("Failed to create a contact");
                eventBus.broadcast(new ContactExchangeFailedEvent());
            }

        } catch (IOException | CryptoException e) {
            logger.warning("Unable to perform contact exchange " + e);
            eventBus.broadcast(new ContactExchangeFailedEvent());
        }
    }

    private boolean storeContact(Identity identity) {
        Contact contact = new Contact(identity.getName(), identity.getPublicKey());

        boolean contactStored = false;
        try {
            contactStored = contactManager.createContact(contact);
        } catch (ContactAlreadyExistsException e) {
            logger.info("Contact already exists");
        }
        return contactStored;
    }

    private void encryptAndSendIdentity(Identity identity) throws IOException, CryptoException {
        byte[] encoded = identityEncoder.encode(identity);
        Pair<SecretKey, Tag> keyAndTag = keyManager.retrieveNextOutgoingKeyAndTag();

        SecretKey key = keyAndTag.getFirst();
        byte[] encrypted = crypto.encryptWithKey(key, encoded);
        EncryptedPacket packet = new EncryptedPacket(keyAndTag.getSecond(), encrypted);

        transportWriter.writePacket(packet);
    }

    private Identity receiveAndDecryptIdentity() throws IOException, DecryptionException {
        EncryptedPacket packet = transportReader.readNextPacket();

        SecretKey key = keyManager.retrieveIncomingKey(packet.getTag());
        byte[] decrypted = crypto.decryptWithKey(key, packet.getContent());

        Identity identity = identityDecoder.decode(decrypted);
        return identity;
    }
}
