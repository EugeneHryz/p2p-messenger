package com.eugene.wc.protocol.contact.exchange;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.exception.ContactAlreadyExistsException;
import com.eugene.wc.protocol.api.contact.exception.ContactExchangeException;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeKeyManager;
import com.eugene.wc.protocol.api.contact.exchange.ContactExchangeManager;
import com.eugene.wc.protocol.api.contact.exchange.ContactInfo;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeResult;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportReader;
import com.eugene.wc.protocol.api.transport.TransportWriter;
import com.eugene.wc.protocol.transport.TransportReaderImpl;
import com.eugene.wc.protocol.transport.TransportWriterImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.logging.Logger;

public class ContactExchangeManagerImpl implements ContactExchangeManager {

    private static final Logger logger = Logger.getLogger(ContactExchangeManagerImpl.class.getName());

    private final IdentityManager identityManager;
    private final ContactExchangeKeyManager keyManager;
    private final CryptoComponent crypto;
    private final ContactManager contactManager;
    private final ClientHelper clientHelper;
    private final TransportPropertyManager tpm;
    private final DatabaseComponent db;

    private final boolean isAlice;

    private TransportReader transportReader;
    private TransportWriter transportWriter;

    public ContactExchangeManagerImpl(KeyExchangeResult result,
                                      IdentityManager identityManager,
                                      CryptoComponent crypto,
                                      ContactManager contactManager,
                                      ClientHelper clientHelper,
                                      TransportPropertyManager tpm,
                                      DatabaseComponent db) {
        this.identityManager = identityManager;
        this.crypto = crypto;
        this.contactManager = contactManager;
        this.clientHelper = clientHelper;
        this.tpm = tpm;
        this.db = db;
        isAlice = result.isAlice();

        keyManager = new ContactExchangeKeyManagerImpl(isAlice, crypto, db);
        keyManager.generateInitialKeys(result.getMasterKey());

        initReaderAndWriter(result.getTransport().getConnection());
    }

    @Override
    public ContactId startContactExchange() throws ContactExchangeException {
        return performExchange();
    }

    private ContactId performExchange() throws ContactExchangeException {
        Identity localIdentity;
        Map<TransportId, TransportProperties> localProps;
        try {
            localIdentity = identityManager.getIdentity();
            localProps = tpm.getLocalProperties();
        } catch (DbException e) {
            logger.warning("Unable to load local identity data\n" + e);
            throw new ContactExchangeException("Unable to load local identity data", e);
        }

        try {
            ContactInfo remoteContactInfo;
            if (isAlice) {
                encryptAndSendContactInfo(localIdentity, localProps);
                remoteContactInfo = receiveAndDecryptContactInfo();
            } else {
                remoteContactInfo = receiveAndDecryptContactInfo();
                encryptAndSendContactInfo(localIdentity, localProps);
            }

            try {
                return addContact(remoteContactInfo, localIdentity.getId());
            } catch (ContactAlreadyExistsException | DbException e) {
                throw new ContactExchangeException("Unable to add contact", e);
            }
        } catch (IOException | CryptoException e) {
            logger.warning("Unable to perform contact exchange " + e);
            throw new ContactExchangeException("Unable to perform contact exchange", e);
        }
    }

    private ContactId addContact(ContactInfo contactInfo, IdentityId localId)
            throws ContactAlreadyExistsException, DbException {
        Connection txn = null;
        ContactId contactId;
        try {
            txn = db.startTransaction(false);

            contactId = contactManager.createContact(txn, contactInfo.getIdentity(), localId);
            keyManager.saveLastGeneratedKeys(txn, contactId);
            tpm.addRemoteProperties(txn, contactId, contactInfo.getProperties());

            db.commitTransaction(txn);
        } catch (DbException | ContactAlreadyExistsException e) {
            logger.info("Unable to add contact\n" + e);
            db.abortTransaction(txn);
            throw e;
        }
        return contactId;
    }

    private void encryptAndSendContactInfo(Identity identity, Map<TransportId, TransportProperties>
            propertiesMap) throws IOException, CryptoException {

        WdfList2 identityList = clientHelper.toList(identity);
        WdfDictionary2 properties = clientHelper.toDictionary(propertiesMap);
        byte[] encoded = clientHelper.toByteArray(WdfList2.of(identityList, properties));

        Pair<SecretKey, Tag> keyAndTag = keyManager.retrieveNextOutgoingKeyAndTag();

        SecretKey key = keyAndTag.getFirst();
        byte[] encrypted = crypto.encryptWithKey(key, encoded);
        EncryptedPacket packet = new EncryptedPacket(keyAndTag.getSecond(), encrypted);

        transportWriter.writePacket(packet);
    }

    private ContactInfo receiveAndDecryptContactInfo() throws IOException, DecryptionException {
        EncryptedPacket packet = transportReader.readNextPacket();

        SecretKey key = keyManager.retrieveIncomingKey(packet.getTag());
        byte[] decrypted = crypto.decryptWithKey(key, packet.getContent());

        WdfList2 payload = clientHelper.toList(decrypted);
        if (payload.size() != 2) {
            throw new AssertionError("Expected list with size 2");
        }
        WdfList2 identityAsList = payload.getList(0);
        WdfDictionary2 propertiesAsDictionary = payload.getDictionary(1);

        Identity identity = clientHelper.parseIdentity(identityAsList);
        Map<TransportId, TransportProperties> propsMap = clientHelper
                .parseTransportPropertiesMap(propertiesAsDictionary);

        return new ContactInfo(identity, propsMap);
    }

    private void initReaderAndWriter(DuplexTransportConnection conn) {
        try {
            OutputStream out = conn.getWriter().getOutputStream();
            InputStream in = conn.getReader().getInputStream();

            transportWriter = new TransportWriterImpl(out);
            transportReader = new TransportReaderImpl(in);

        } catch (IOException e) {
            logger.severe("Unable to initialize TransportReader and TransportWriter\n" + e);
            throw new RuntimeException(e);
        }
    }
}
