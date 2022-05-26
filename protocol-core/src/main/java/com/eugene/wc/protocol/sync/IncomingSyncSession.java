package com.eugene.wc.protocol.sync;

import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.contact.event.ContactRemovedEvent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.identity.IdentityId;
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
import java.util.logging.Logger;

public class IncomingSyncSession extends Thread implements EventListener, SyncSessionCallback {

    private static final Logger logger = Logger.getLogger(OutgoingSyncSession.class.getName());

    private ContactId contactId;
    private final DuplexTransportConnection connection;
    private final TransportId transportId;

    private final ConnectionRegistry connectionRegistry;
    private final TransportPropertyManager tpm;
    private final EventBus eventBus;
    private final ContactManager contactManager;

    private SessionWriter sessionWriter;
    private SessionReader sessionReader;

    public IncomingSyncSession(DuplexTransportConnection c, TransportId transportId,
                               ConnectionRegistry connectionRegistry,
                               TransportPropertyManager tpm, EventBus eventBus,
                               ContactManager contactManager) {
        connection = c;
        this.transportId = transportId;
        this.connectionRegistry = connectionRegistry;
        this.tpm = tpm;
        this.eventBus = eventBus;
        this.contactManager = contactManager;

        try {
            OutputStream output = c.getWriter().getOutputStream();
            InputStream input = c.getReader().getInputStream();

            sessionReader = new SessionReader(new WdfReaderImpl(input), this);
            sessionWriter = new SessionWriter(new WdfWriterImpl(output));
        } catch (IOException e) {
            logger.warning("Unable to get output stream from Writer\n" + e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        eventBus.addListener(this);
        try {
            sessionReader.start();
            sessionWriter.start();
        } finally {

            try {
                sessionReader.join();

                sessionWriter.setInterrupted(true);
                sessionWriter.join();
            } catch (InterruptedException e) {
                logger.warning("Interrupted while waiting for thread to finish\n" + e);
            }
            eventBus.removeListener(this);
            if (contactId != null) {
                connectionRegistry.unregisterConnection(contactId, transportId, connection,
                        true, false);
            }
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

    @Override
    public void onRemoteIdReceived(byte[] remoteIdBytes) {
        IdentityId remoteId = new IdentityId(remoteIdBytes);

        Contact contact = contactManager.getContact(remoteId);
        contactId = contact.getId();
        registerContactConnection();
    }

    private void registerContactConnection() {
        connectionRegistry.registerIncomingConnection(contactId, transportId, connection);
        try {
            tpm.addRemotePropertiesFromConnection(contactId, transportId, connection
                    .getRemoteProperties());
        } catch (DbException e) {
            logger.warning("Unable add remote properties from connection\n" + e);
        }
    }
}
