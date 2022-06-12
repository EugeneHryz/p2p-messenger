package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.event.ContactRemovedEvent;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.plugin.event.TransportInactiveEvent;
import com.eugene.wc.protocol.api.session.MessageReader;
import com.eugene.wc.protocol.api.session.MessageWriter;
import com.eugene.wc.protocol.api.session.event.CloseSyncConnectionsEvent;
import com.eugene.wc.protocol.api.util.ArrayUtil;

import java.util.concurrent.CountDownLatch;

public abstract class MessageSession implements Runnable, EventListener, SessionReaderCallback {

    public interface Callback {

        /**
         * This method will close one session if there are more than one session
         * registered for a given contactId. This method must be called only if
         * we are responsible for closing redundant session {@link #shouldCloseRedundantSession()}
         *
         * @param contactId contactId this session is associated with
         */
        void closeRedundantSessionIfNeeded(ContactId contactId, MessageSession initiator);
    }

    protected Contact contact;
    protected DuplexTransportConnection connection;
    protected TransportId transportId;

    protected MessageWriter messageWriter;
    protected MessageReader messageReader;

    protected SessionWriter sessionWriter;
    protected SessionReader sessionReader;

    protected final CountDownLatch closeLatch = new CountDownLatch(1);

    protected Callback callback;


    /**
     * This method interrupts session's reader and writer
     * and waits for it to finish execution
     *
     * @throws InterruptedException if calling thread was interrupted while waiting
     */
    public void closeSession() throws InterruptedException {
        if (sessionWriter != null) {
            sessionWriter.setInterrupted(true);
        }
        if (sessionReader != null) {
            sessionReader.setInterrupted(true);
        }
        closeLatch.await();
    }

    @Override
    public void onEventOccurred(Event e) {
        boolean needInterrupt = false;

        if (e instanceof ContactRemovedEvent) {
            needInterrupt = true;
        } else if (e instanceof TransportInactiveEvent) {
            needInterrupt = true;
        } else if (e instanceof CloseSyncConnectionsEvent) {
            CloseSyncConnectionsEvent event = (CloseSyncConnectionsEvent) e;
            if (event.getTransportId().equals(transportId)) {
                needInterrupt = true;
            }
        }
        if (needInterrupt) {
            sessionReader.setInterrupted(true);
            sessionWriter.setInterrupted(true);
        }
    }

    /**
     * This method must be called after contact object is initialized.
     *
     * @return true, if we are responsible for closing redundant session, false otherwise
     */
    protected final boolean shouldCloseRedundantSession() {
        if (contact == null) {
            throw new IllegalStateException("contact must be initialized before " +
                    "calling this method");
        }
        byte[] localIdBytes = contact.getLocalIdentityId().getBytes();
        byte[] remoteIdBytes = contact.getIdentity().getId().getBytes();

        return ArrayUtil.compare(localIdBytes, 0, remoteIdBytes, 0,
                IdentityId.LENGTH) > 0;
    }

    /**
     * @return contactId, or null if contact is not yet initialized
     */
    public final ContactId getContactId() {
        if (contact != null) {
            return contact.getId();
        }
        return null;
    }
}
