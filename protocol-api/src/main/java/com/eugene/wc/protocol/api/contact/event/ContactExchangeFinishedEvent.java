package com.eugene.wc.protocol.api.contact.event;

import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.Event;

public class ContactExchangeFinishedEvent extends Event {

    private final ContactId contactId;

    public ContactExchangeFinishedEvent(ContactId contactId) {
        this.contactId = contactId;
    }

    public ContactId getContactId() {
        return contactId;
    }
}
