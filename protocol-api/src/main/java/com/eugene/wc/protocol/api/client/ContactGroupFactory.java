package com.eugene.wc.protocol.api.client;

import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.sync.ClientId;
import com.eugene.wc.protocol.api.sync.Group;

public interface ContactGroupFactory {

	/**
	 * Creates a group that is not shared with any contacts.
	 */
	Group createLocalGroup(ClientId clientId, int majorVersion);

	Group createContactGroup(ClientId clientId, int majorVersion, Contact contact);

}
