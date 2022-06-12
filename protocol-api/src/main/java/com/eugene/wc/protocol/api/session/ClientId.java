package com.eugene.wc.protocol.api.session;

import com.eugene.wc.protocol.api.util.StringUtils;

/**
 * Type-safe wrapper for a namespaced string that uniquely identifies a sync
 * client.
 */
public class ClientId implements Comparable<ClientId> {

	/**
	 * The maximum length of a client identifier in UTF-8 bytes.
	 */
	public static int MAX_CLIENT_ID_LENGTH = 100;

	private final String id;

	public ClientId(String id) {
		int length = StringUtils.toUtf8(id).length;
		if (length == 0 || length > MAX_CLIENT_ID_LENGTH)
			throw new IllegalArgumentException();
		this.id = id;
	}

	public String getString() {
		return id;
	}

	@Override
	public int compareTo(ClientId clientId) {
		return id.compareTo(clientId.getString());
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ClientId && id.equals(((ClientId) o).id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id;
	}
}
