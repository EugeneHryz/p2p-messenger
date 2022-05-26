package com.eugene.wc.protocol.api.sync;

public interface GroupFactory {

	/**
	 * Creates a group with the given client ID, major version and descriptor.
	 */
	Group createGroup(ClientId c, int majorVersion, byte[] descriptor);
}
