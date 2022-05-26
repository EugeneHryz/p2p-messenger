package com.eugene.wc.protocol.api.contact.event;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.Event;

public class ContactVerifiedEvent extends Event {

	private final ContactId contactId;

	public ContactVerifiedEvent(ContactId contactId) {
		this.contactId = contactId;
	}

	public ContactId getContactId() {
		return contactId;
	}
}
