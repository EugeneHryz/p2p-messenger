package com.eugene.wc.protocol.api.plugin.event;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.Event;

/**
 * An event that is broadcast when a contact connects that was not previously
 * connected via any transport.
 */
public class ContactConnectedEvent extends Event {

	private final ContactId contactId;

	public ContactConnectedEvent(ContactId contactId) {
		this.contactId = contactId;
	}

	public ContactId getContactId() {
		return contactId;
	}
}
