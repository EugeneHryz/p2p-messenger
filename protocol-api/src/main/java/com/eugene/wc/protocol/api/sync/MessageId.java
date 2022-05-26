package com.eugene.wc.protocol.api.sync;

import com.eugene.wc.protocol.api.UniqueId;

public class MessageId extends UniqueId {

	/**
	 * Label for hashing messages to calculate their identifiers.
	 */
	public static final String ID_LABEL = "com.eugene.wc.protocol/MESSAGE_ID";

	/**
	 * Label for hashing blocks of messages.
	 */
	public static final String BLOCK_LABEL =
			"com.eugene.wc.protocol/MESSAGE_BLOCK";

	public MessageId(byte[] id) {
		super(id);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof MessageId && super.equals(o);
	}
}
