package com.eugene.wc.protocol.api.client;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.session.ClientId;
import com.eugene.wc.protocol.api.session.Group;

public interface ContactGroupFactory {

	Group createLocalGroup(ClientId clientId);

	Group createContactGroup(ClientId clientId, Contact contact);
}
