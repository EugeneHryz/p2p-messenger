package com.eugene.wc.protocol.api.plugin.event;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.plugin.TransportId;

public class ConnectionOpenedEvent extends Event {

	private final ContactId contactId;
	private final TransportId transportId;
	private final boolean incoming;

	public ConnectionOpenedEvent(ContactId contactId, TransportId transportId,
			boolean incoming) {
		this.contactId = contactId;
		this.transportId = transportId;
		this.incoming = incoming;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public TransportId getTransportId() {
		return transportId;
	}

	public boolean isIncoming() {
		return incoming;
	}
}
