package com.eugene.wc.protocol.session;

import static com.eugene.wc.protocol.api.session.SyncConstants.PRIORITY_NONCE_BYTES;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.connection.Priority;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.conversation.ContactConversation;
import com.eugene.wc.protocol.api.conversation.ConversationManager;
import com.eugene.wc.protocol.api.crypto.exception.CryptoException;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.api.session.Ack;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.transport.KeyManager;
import com.eugene.wc.protocol.transport.TransportKeyManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.logging.Logger;

public class OutgoingMessageSession extends MessageSession {

    private static final Logger logger = Logger.getLogger(OutgoingMessageSession.class.getName());

    /**
     * how much time we will wait for another device to recognise contact connection
     */
    private static final int ACK_TIMEOUT = 5000;

    private final ConnectionRegistry connectionRegistry;
    private final TransportPropertyManager tpm;
    private final SecureRandom secureRandom;
    private final EventBus eventBus;
    private final KeyManager transportKeyManager;

    private final ProtocolComponent component;

    private final Message introMessage;
    private ContactConversation contactConversation;

    public OutgoingMessageSession(Callback callback,
                                  DuplexTransportConnection conn,
                                  TransportId transportId,
                                  ContactId contactId,
                                  ConnectionRegistry connectionRegistry,
                                  TransportPropertyManager tpm,
                                  EventBus eventBus,
                                  SecureRandom secureRandom,
                                  ContactManager contactManager,
                                  ConversationManager conversationManager,
                                  MessageFactory messageFactory,
                                  ContactGroupFactory contactGroupFactory,
                                  ProtocolComponent component) {
        this.callback = callback;
        connection = conn;
        this.transportId = transportId;
        this.connectionRegistry = connectionRegistry;
        this.tpm = tpm;
        this.secureRandom = secureRandom;
        this.eventBus = eventBus;
        this.component = component;
        this.transportKeyManager = new TransportKeyManager(contactId, component);

        try {
            contact = contactManager.getContactById(contactId);
        } catch (DbException e) {
            logger.warning("Unable to get contact by id\n" + e);
            throw new RuntimeException(e);
        }
        contactConversation = conversationManager.getContactConversation(contactId);
        if (contactConversation == null) {
            contactConversation = conversationManager.registerConversation(contact);
        }

        Group sharedGroup = contactGroupFactory.createContactGroup(ConversationManager.CLIENT_ID, contact);
        long now = System.currentTimeMillis();
        byte[] contentRaw = contact.getLocalIdentityId().getBytes();

        introMessage = messageFactory.createMessage(sharedGroup.getId(), now, contentRaw);

        initReaderAndWriter();
    }

    @Override
    public void run() {
        try {
            if (!introduceItself()) {
                messageWriter.sendEndOfSession();
                logger.info("Timeout for Ack ended or expected Ack content is invalid");
                return;
            }
        } catch (IOException | CryptoException e) {
            logger.warning("Unable to introduce itself\n" + e);
            return;
        }

        sessionWriter = new SessionWriter(messageWriter, contactConversation.getMessageQueue());
        sessionReader = new SessionReader(messageReader, contactConversation);

        if (shouldCloseRedundantSession()) {
            logger.info("We are responsible for closing redundant session");
            callback.closeRedundantSessionIfNeeded(contact.getId(), this);
        }
        registerContactConnection();
        eventBus.addListener(this);
        try {
            sessionWriter.start();
            sessionReader.start();

        } finally {
            try {
                sessionReader.join();

                sessionWriter.setInterrupted(true);
                sessionWriter.join();
            } catch (InterruptedException e) {
                logger.warning("Interrupted while waiting for thread to finish\n" + e);
            }
            eventBus.removeListener(this);
            connectionRegistry.unregisterConnection(contact.getId(), transportId, connection,
                    false, false);

            closeLatch.countDown();
        }
    }

    private boolean introduceItself() throws IOException, CryptoException {
        messageWriter.sendMessage(introMessage);

        long elapsedTime = 0;
        long start = System.currentTimeMillis();
        boolean acked = false;
        while (elapsedTime < ACK_TIMEOUT && !acked) {
            if (messageReader.hasAck()) {
                Ack ack = messageReader.readNextAck();

                if (ack.getLocalId().equals(contact.getIdentity().getId()) &&
                        ack.getRemoteId().equals(contact.getLocalIdentityId())) {
                    acked = true;
                }
            }
            elapsedTime += (System.currentTimeMillis() - start);
        }
        return acked;
    }

    private void registerContactConnection() {
        Priority priority = generatePriority();
        connectionRegistry.registerOutgoingConnection(contact.getId(), transportId, connection, priority);
        try {
            TransportProperties tProps = connection.getRemoteProperties();
            tpm.addRemotePropertiesFromConnection(contact.getId(), transportId, tProps);
        } catch (DbException e) {
            logger.warning("Unable add remote properties from connection\n" + e);
        }
    }

    private void initReaderAndWriter() {
        try {
            OutputStream output = connection.getWriter().getOutputStream();
            InputStream input = connection.getReader().getInputStream();

            messageWriter = new MessageWriterImpl(output, transportKeyManager, component);
            messageReader = new MessageReaderImpl(input, transportKeyManager, component);
        } catch (IOException e) {
            logger.warning("Unable to get output stream from Writer\n" + e);
            throw new RuntimeException(e);
        }
    }

    private Priority generatePriority() {
        byte[] nonce = new byte[PRIORITY_NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        return new Priority(nonce);
    }
}
