package com.eugene.wc.protocol.api.contact.event;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.Event;

/**
 * An event that is broadcast when a contact is added.
 */
public class ContactAddedEvent extends Event {

	private final ContactId contactId;
	private final boolean verified;

	public ContactAddedEvent(ContactId contactId, boolean verified) {
		this.contactId = contactId;
		this.verified = verified;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public boolean isVerified() {
		return verified;
	}
}
