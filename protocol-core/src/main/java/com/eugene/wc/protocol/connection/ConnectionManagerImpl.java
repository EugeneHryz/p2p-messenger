package com.eugene.wc.protocol.connection;

import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;
import com.eugene.wc.protocol.api.connection.ConnectionManager;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.conversation.ConversationManager;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.session.IncomingMessageSession;
import com.eugene.wc.protocol.session.MessageSession;
import com.eugene.wc.protocol.session.OutgoingMessageSession;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;

public class ConnectionManagerImpl implements ConnectionManager, MessageSession.Callback {

    private static final Logger logger = Logger.getLogger(ConnectionManagerImpl.class.getName());

    private final Executor ioExecutor;
    private final ConnectionRegistry connectionRegistry;
    private final TransportPropertyManager tpm;
    private final EventBus eventBus;
    private final SecureRandom secureRandom;
    private final ContactManager contactManager;
    private final MessageFactory messageFactory;
    private final ConversationManager conversationManager;
    private final ContactGroupFactory contactGroupFactory;
    private final ProtocolComponent component;

    private final Lock sessionsLock = new ReentrantLock();

    @GuardedBy("sessionsLock")
    private final List<MessageSession> activeSessions = new ArrayList<>();

    @Inject
    public ConnectionManagerImpl(@IoExecutor Executor ioExecutor,
                                 ConnectionRegistry connectionRegistry,
                                 TransportPropertyManager tpm,
                                 EventBus eventBus,
                                 SecureRandom secureRandom,
                                 ContactManager contactManager,
                                 MessageFactory messageFactory,
                                 ConversationManager conversationManager,
                                 ContactGroupFactory contactGroupFactory,
                                 ProtocolComponent component) {
        this.ioExecutor = ioExecutor;
        this.connectionRegistry = connectionRegistry;
        this.tpm = tpm;
        this.eventBus = eventBus;
        this.secureRandom = secureRandom;
        this.contactManager = contactManager;
        this.messageFactory = messageFactory;
        this.conversationManager = conversationManager;
        this.contactGroupFactory = contactGroupFactory;
        this.component = component;
    }

    @Override
    public void manageIncomingConnection(DuplexTransportConnection c, TransportId transportId) {
        submitSession(new IncomingMessageSession(this, c, transportId, connectionRegistry,
                tpm, eventBus, contactManager, conversationManager, component));
    }

    @Override
    public void manageOutgoingConnection(DuplexTransportConnection c, TransportId transportId,
                                         ContactId contactId) {
        submitSession(new OutgoingMessageSession(this, c, transportId, contactId,
                connectionRegistry, tpm, eventBus, secureRandom, contactManager,
                conversationManager, messageFactory, contactGroupFactory, component));
    }

    @Override
    public void closeRedundantSessionIfNeeded(ContactId contactId, MessageSession initiator) {
        if (sessionsLock.tryLock()) {
            try {
                MessageSession anotherSession = null;
                for (MessageSession ms : activeSessions) {
                    ContactId cId = ms.getContactId();
                    if (cId != null && cId.equals(contactId)) {
                        if (!ms.equals(initiator)) {
                            anotherSession = ms;
                            break;
                        }
                    }
                }
                if (anotherSession != null) {
                    logger.info("Found another session for a given contactId");
                    try {
                        logger.info("About to close another session");
                        anotherSession.closeSession();
                        removeSession(anotherSession);
                    } catch (InterruptedException e) {
                        logger.warning("Interrupted while waiting for a " +
                                "MessageSession to close\n" + e);
                    }
                }
            } finally {
                sessionsLock.unlock();
            }
        }
    }

    private void submitSession(MessageSession session) {
        sessionsLock.lock();
        try {
            activeSessions.add(session);
        } finally {
            sessionsLock.unlock();
        }
        ioExecutor.execute(() -> {
            session.run();
            removeSession(session);
        });
    }

    private void removeSession(MessageSession session) {
        sessionsLock.lock();
        try {
            activeSessions.remove(session);
        } finally {
            sessionsLock.unlock();
        }
    }
}
