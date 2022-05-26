package com.eugene.wc.protocol.client;

import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.identity.IdentityId;
import com.eugene.wc.protocol.api.sync.ClientId;
import com.eugene.wc.protocol.api.sync.Group;
import com.eugene.wc.protocol.api.sync.GroupFactory;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class ContactGroupFactoryImpl implements ContactGroupFactory {

	private static final byte[] LOCAL_GROUP_DESCRIPTOR = new byte[0];

	private final GroupFactory groupFactory;
	private final ClientHelper clientHelper;

	@Inject
    public ContactGroupFactoryImpl(GroupFactory groupFactory,
                            ClientHelper clientHelper) {
		this.groupFactory = groupFactory;
		this.clientHelper = clientHelper;
	}

	@Override
	public Group createLocalGroup(ClientId clientId, int majorVersion) {
		return groupFactory.createGroup(clientId, majorVersion,
				LOCAL_GROUP_DESCRIPTOR);
	}

	@Override
	public Group createContactGroup(ClientId clientId, int majorVersion, Contact contact) {
		IdentityId local = contact.getLocalIdentityId();
		IdentityId remote = contact.getIdentity().getId();
		byte[] descriptor = createGroupDescriptor(local, remote);
		return groupFactory.createGroup(clientId, majorVersion, descriptor);
	}

	private byte[] createGroupDescriptor(IdentityId local, IdentityId remote) {
		try {
			if (local.compareTo(remote) < 0)
				return clientHelper.toByteArray(WdfList2.of(local, remote));
			else return clientHelper.toByteArray(WdfList2.of(remote, local));
		} catch (FormatException e) {
			throw new RuntimeException(e);
		}
	}
}
