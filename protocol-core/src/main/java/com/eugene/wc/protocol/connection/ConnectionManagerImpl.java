package com.eugene.wc.protocol.connection;

import com.eugene.wc.protocol.api.connection.ConnectionManager;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.sync.IncomingSyncSession;
import com.eugene.wc.protocol.sync.OutgoingSyncSession;

import java.security.SecureRandom;
import java.util.concurrent.Executor;

import javax.inject.Inject;

public class ConnectionManagerImpl implements ConnectionManager {

    private final Executor ioExecutor;
    private final ConnectionRegistry connectionRegistry;
    private final TransportPropertyManager tpm;
    private final EventBus eventBus;
    private final SecureRandom secureRandom;
    private final IdentityManager identityManager;
    private final ContactManager contactManager;

    @Inject
    public ConnectionManagerImpl(@IoExecutor Executor ioExecutor,
                                 ConnectionRegistry connectionRegistry,
                                 TransportPropertyManager tpm,
                                 EventBus eventBus,
                                 SecureRandom secureRandom,
                                 IdentityManager identityManager,
                                 ContactManager contactManager) {
        this.ioExecutor = ioExecutor;
        this.connectionRegistry = connectionRegistry;
        this.tpm = tpm;
        this.eventBus = eventBus;
        this.secureRandom = secureRandom;
        this.identityManager = identityManager;
        this.contactManager = contactManager;
    }

    @Override
    public void manageIncomingConnection(DuplexTransportConnection c, TransportId transportId) {
        ioExecutor.execute(new IncomingSyncSession(c, transportId, connectionRegistry,
                tpm, eventBus, contactManager));
    }

    @Override
    public void manageOutgoingConnection(DuplexTransportConnection c, TransportId transportId,
                                         ContactId contactId) {
        ioExecutor.execute(new OutgoingSyncSession(contactId, c, transportId,
                connectionRegistry, tpm, eventBus, secureRandom, identityManager));
    }
}
