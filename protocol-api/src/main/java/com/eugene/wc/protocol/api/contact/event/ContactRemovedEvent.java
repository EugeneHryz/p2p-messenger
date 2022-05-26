package com.eugene.wc.protocol.api.contact.event;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.Event;

public class ContactRemovedEvent extends Event {

	private final ContactId contactId;

	public ContactRemovedEvent(ContactId contactId) {
		this.contactId = contactId;
	}

	public ContactId getContactId() {
		return contactId;
	}
}
