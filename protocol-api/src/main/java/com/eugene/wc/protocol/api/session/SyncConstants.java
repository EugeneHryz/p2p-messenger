package com.eugene.wc.protocol.api.session;

import com.eugene.wc.protocol.api.UniqueId;

public interface SyncConstants {

	/**
	 * The maximum length of a group descriptor in bytes.
	 */
	int MAX_GROUP_DESCRIPTOR_LENGTH = 16 * 1024; // 16 KiB

	/**
	 * The length of the message header in bytes.
	 */
	int MESSAGE_HEADER_LENGTH = UniqueId.LENGTH + 8;

	/**
	 * The maximum length of a message body in bytes.
	 */
	int MAX_MESSAGE_BODY_LENGTH = 32 * 1024; // 32 KiB

	/**
	 * The maximum length of a message in bytes.
	 */
	int MAX_MESSAGE_LENGTH = MESSAGE_HEADER_LENGTH + MAX_MESSAGE_BODY_LENGTH;

	/**
	 * The length of the priority nonce used for choosing between redundant
	 * connections.
	 */
	int PRIORITY_NONCE_BYTES = 16;

}
