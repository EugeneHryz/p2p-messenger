package com.eugene.wc.protocol.sync;

import static com.eugene.wc.protocol.api.sync.SyncConstants.PRIORITY_NONCE_BYTES;

import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.connection.Priority;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.event.ContactRemovedEvent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.plugin.event.TransportInactiveEvent;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.api.sync.event.CloseSyncConnectionsEvent;
import com.eugene.wc.protocol.data.WdfReaderImpl;
import com.eugene.wc.protocol.data.WdfWriterImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OutgoingSyncSession extends Thread implements EventListener {

    private static final Logger logger = Logger.getLogger(OutgoingSyncSession.class.getName());

    private final ContactId contactId;
    private final DuplexTransportConnection connection;
    private final TransportId transportId;

    private final ConnectionRegistry connectionRegistry;
    private final TransportPropertyManager tpm;
    private final SecureRandom secureRandom;
    private final EventBus eventBus;
    private final IdentityManager identityManager;

    private SessionWriter sessionWriter;
    private SessionReader sessionReader;

    public OutgoingSyncSession(ContactId contactId, DuplexTransportConnection c,
                               TransportId transportId, ConnectionRegistry connectionRegistry,
                               TransportPropertyManager tpm, EventBus eventBus,
                               SecureRandom secureRandom, IdentityManager identityManager) {
        this.contactId = contactId;
        connection = c;
        this.transportId = transportId;
        this.connectionRegistry = connectionRegistry;
        this.tpm = tpm;
        this.secureRandom = secureRandom;
        this.eventBus = eventBus;
        this.identityManager = identityManager;

        Identity localIdentity;
        try {
            localIdentity = identityManager.getIdentity();
        } catch (DbException e) {
            logger.warning("Unable to get local identity\n" + e);
            throw new RuntimeException(e);
        }

        try {
            OutputStream output = c.getWriter().getOutputStream();
            InputStream input = c.getReader().getInputStream();

            sessionWriter = new SessionWriter(new WdfWriterImpl(output), localIdentity.getId());
            sessionReader = new SessionReader(new WdfReaderImpl(input));
        } catch (IOException e) {
            logger.warning("Unable to get output stream from Writer\n" + e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
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
            connectionRegistry.unregisterConnection(contactId, transportId, connection,
                    false, false);
        }
    }

    @Override
    public void onEventOccurred(Event e) {
        if (e instanceof ContactRemovedEvent) {
            sessionWriter.setInterrupted(true);
            sessionReader.setInterrupted(true);

        } else if (e instanceof TransportInactiveEvent) {
            sessionWriter.setInterrupted(true);
            sessionReader.setInterrupted(true);
        } else if (e instanceof CloseSyncConnectionsEvent) {
            CloseSyncConnectionsEvent event = (CloseSyncConnectionsEvent) e;

            if (event.getTransportId().equals(transportId)) {
                sessionWriter.setInterrupted(true);
                sessionReader.setInterrupted(true);
            }
        }
    }

    private void registerContactConnection() {
        Priority priority = generatePriority();
        connectionRegistry.registerOutgoingConnection(contactId, transportId, connection, priority);
        try {
            tpm.addRemotePropertiesFromConnection(contactId, transportId, connection
                    .getRemoteProperties());
        } catch (DbException e) {
            logger.warning("Unable add remote properties from connection\n" + e);
        }
    }

    private Priority generatePriority() {
        byte[] nonce = new byte[PRIORITY_NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        return new Priority(nonce);
    }
}
