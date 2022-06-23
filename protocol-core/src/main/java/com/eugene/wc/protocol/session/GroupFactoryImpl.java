package com.eugene.wc.protocol.session;

import static com.eugene.wc.protocol.api.session.GroupId.LABEL;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.session.ClientId;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.GroupFactory;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.util.StringUtils;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class GroupFactoryImpl implements GroupFactory {

	private static final byte[] FORMAT_VERSION_BYTES = new byte[] { Group.FORMAT_VERSION };

	private final CryptoComponent crypto;

	@Inject
    public GroupFactoryImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public Group createGroup(ClientId c, byte[] descriptor) {
		byte[] hash = crypto.hash(LABEL, FORMAT_VERSION_BYTES,
				StringUtils.toUtf8(c.getString()), descriptor);
		return new Group(new GroupId(hash), c, descriptor);
	}
}
