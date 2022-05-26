package com.eugene.wc.protocol.api.sync;

import com.eugene.wc.protocol.api.UniqueId;

public class GroupId extends UniqueId {

	/**
	 * Label for hashing groups to calculate their identifiers.
	 */
	public static final String LABEL = "com.eugene.wc.protocol/GROUP_ID";

	public GroupId(byte[] id) {
		super(id);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof GroupId && super.equals(o);
	}
}
