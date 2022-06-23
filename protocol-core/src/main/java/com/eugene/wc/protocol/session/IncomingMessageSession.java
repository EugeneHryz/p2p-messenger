package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
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
import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.KeyManager;
import com.eugene.wc.protocol.api.transport.TransportReader;
import com.eugene.wc.protocol.transport.TransportKeyManager;
import com.eugene.wc.protocol.transport.TransportReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class IncomingMessageSession extends MessageSession {

    private static final Logger logger = Logger.getLogger(IncomingMessageSession.class.getName());

    private final ConnectionRegistry connectionRegistry;
    private final TransportPropertyManager tpm;
    private final EventBus eventBus;
    private final ContactManager contactManager;
    private final ConversationManager conversationManager;

    private final ProtocolComponent component;

    public IncomingMessageSession(Callback callback,
                                  DuplexTransportConnection conn,
                                  TransportId transportId,
                                  ConnectionRegistry connectionRegistry,
                                  TransportPropertyManager tpm,
                                  EventBus eventBus,
                                  ContactManager contactManager,
                                  ConversationManager conversationManager,
                                  ProtocolComponent component) {
        this.callback = callback;
        connection = conn;
        this.transportId = transportId;
        this.connectionRegistry = connectionRegistry;
        this.tpm = tpm;
        this.eventBus = eventBus;
        this.contactManager = contactManager;
        this.conversationManager = conversationManager;
        this.component = component;
    }

    @Override
    public void run() {
        try {
            if (!recogniseContact()) {
                logger.warning("Unable to recognise contact");
                return;
            }
        } catch (IOException | CryptoException | DbException e) {
            logger.warning("Unable to recognise contact\n" + e);
            return;
        }

        ContactConversation conversation = conversationManager.getContactConversation(contact.getId());
        if (conversation == null) {
            conversation = conversationManager.registerConversation(contact);
        }
        sessionWriter = new SessionWriter(messageWriter, conversation.getMessageQueue());
        sessionReader = new SessionReader(messageReader, conversation);

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
            if (contact != null) {
                connectionRegistry.unregisterConnection(contact.getId(), transportId, connection,
                        true, false);
            }
            closeLatch.countDown();
        }
    }

    private boolean recogniseContact() throws IOException, CryptoException, DbException {
        InputStream input = connection.getReader().getInputStream();
        TransportReader transportReader = new TransportReaderImpl(input);

        EncryptedPacket introPacket = transportReader.readNextPacket();
        ContactId contactId = contactManager.recogniseContact(introPacket.getTag());
        boolean recognised = false;
        if (contactId != null) {
            contact = contactManager.getContactById(contactId);

            KeyManager transportKeyManager = new TransportKeyManager(contactId, component);
            OutputStream output = connection.getWriter().getOutputStream();
            messageWriter = new MessageWriterImpl(output, transportKeyManager, component);
            messageReader = new MessageReaderImpl(input, transportKeyManager, component);

            Ack ack = new Ack(contact.getLocalIdentityId(), contact.getIdentity().getId());
            messageWriter.sendAck(ack);
            recognised = true;
        }
        return recognised;
    }

    private void registerContactConnection() {
        connectionRegistry.registerIncomingConnection(contact.getId(), transportId, connection);
        try {
            TransportProperties remoteProps = connection.getRemoteProperties();
            tpm.addRemotePropertiesFromConnection(contact.getId(), transportId, remoteProps);
        } catch (DbException e) {
            logger.warning("Unable add remote properties from connection\n" + e);
        }
    }
}
