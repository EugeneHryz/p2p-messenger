package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.UniqueId;

public class IdentityId extends UniqueId {

	public static final String LABEL = "com.eugene.wc.protocol/IDENTITY_ID";

	public IdentityId(byte[] id) {
		super(id);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof IdentityId && super.equals(o);
	}
}
