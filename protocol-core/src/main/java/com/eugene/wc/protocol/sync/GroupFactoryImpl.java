package com.eugene.wc.protocol.sync;

import static com.eugene.wc.protocol.api.sync.GroupId.LABEL;
import static com.eugene.wc.protocol.api.util.ByteUtils.INT_32_BYTES;

import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.sync.ClientId;
import com.eugene.wc.protocol.api.sync.Group;
import com.eugene.wc.protocol.api.sync.GroupFactory;
import com.eugene.wc.protocol.api.sync.GroupId;
import com.eugene.wc.protocol.api.util.ByteUtils;
import com.eugene.wc.protocol.api.util.StringUtils;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
class GroupFactoryImpl implements GroupFactory {

	private static final byte[] FORMAT_VERSION_BYTES =
			new byte[] { Group.FORMAT_VERSION };

	private final CryptoComponent crypto;

	@Inject
    GroupFactoryImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public Group createGroup(ClientId c, int majorVersion, byte[] descriptor) {
		byte[] majorVersionBytes = new byte[INT_32_BYTES];
		ByteUtils.writeUint32(majorVersion, majorVersionBytes, 0);
		byte[] hash = crypto.hash(LABEL, FORMAT_VERSION_BYTES,
				StringUtils.toUtf8(c.getString()), majorVersionBytes,
				descriptor);
		return new Group(new GroupId(hash), c, majorVersion, descriptor);
	}
}
