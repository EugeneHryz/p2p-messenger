package com.eugene.wc.protocol.api.connection;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

import java.util.Collection;

/**
 * Keeps track of which contacts are currently connected by which transports.
 */
public interface ConnectionRegistry {

	void registerIncomingConnection(ContactId c, TransportId t, DuplexTransportConnection conn);

	void registerOutgoingConnection(ContactId c, TransportId t, DuplexTransportConnection conn,
									Priority priority);

	void unregisterConnection(ContactId c, TransportId t, DuplexTransportConnection conn,
							  boolean incoming, boolean exception);

	void setPriority(ContactId c, TransportId t, DuplexTransportConnection conn,
					 Priority priority);

	Collection<ContactId> getConnectedContacts(TransportId t);

	Collection<ContactId> getConnectedOrBetterContacts(TransportId t);

	boolean isConnected(ContactId c, TransportId t);

	boolean isConnected(ContactId c);
}
