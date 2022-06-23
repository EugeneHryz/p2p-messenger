package com.eugene.wc.protocol.api.properties;

public interface TransportPropertyConstants {

	/**
	 * The maximum length of a property's key or value in UTF-8 bytes.
	 */
	int MAX_PROPERTY_LENGTH = 100;

	/**
	 * Prefix for keys that represent reflected properties.
	 */
	String REFLECTED_PROPERTY_PREFIX = "u:";

	/**
	 * Message metadata key for the transport ID of a local or remote update,
	 * as a WDF string.
	 */
	String MSG_KEY_TRANSPORT_ID = "transportId";

	/**
	 * Message metadata key for the version number of a local or remote update,
	 * as a WDF long.
	 */
	String MSG_KEY_VERSION = "version";

	/**
	 * Message metadata key for whether an update is local or remote, as a WDF
	 * boolean.
	 */
	String MSG_KEY_LOCAL = "local";

	/**
	 * Group metadata key for any discovered transport properties of the
	 * contact, as a WDF dictionary.
	 */
	String GROUP_KEY_DISCOVERED = "discovered";
}
