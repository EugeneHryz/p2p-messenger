package com.eugene.wc.protocol.api.session;

import static com.eugene.wc.protocol.api.session.SyncConstants.MAX_GROUP_DESCRIPTOR_LENGTH;

public class Group {

	/**
	 * The current version of the group format.
	 */
	public static final int FORMAT_VERSION = 1;

	private final GroupId id;
	private final ClientId clientId;
	private final byte[] descriptor;

	public Group(GroupId id, ClientId clientId,
                 byte[] descriptor) {
		if (descriptor.length > MAX_GROUP_DESCRIPTOR_LENGTH)
			throw new IllegalArgumentException();
		this.id = id;
		this.clientId = clientId;
		this.descriptor = descriptor;
	}

	/**
	 * Returns the group's unique identifier.
	 */
	public GroupId getId() {
		return id;
	}

	/**
	 * Returns the ID of the client to which the group belongs.
	 */
	public ClientId getClientId() {
		return clientId;
	}

	/**
	 * Returns the group's descriptor.
	 */
	public byte[] getDescriptor() {
		return descriptor;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Group && id.equals(((Group) o).id);
	}
}
