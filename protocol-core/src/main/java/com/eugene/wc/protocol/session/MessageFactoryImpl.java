package com.eugene.wc.protocol.session;

import static com.eugene.wc.protocol.api.session.MessageId.*;
import static com.eugene.wc.protocol.api.session.SyncConstants.*;
import static com.eugene.wc.protocol.api.util.ByteUtils.INT_64_BYTES;

import com.eugene.wc.protocol.api.UniqueId;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.session.MessageId;
import com.eugene.wc.protocol.api.util.ByteUtils;

import javax.inject.Inject;

class MessageFactoryImpl implements MessageFactory {

	private static final byte[] FORMAT_VERSION_BYTES = new byte[] { Message.FORMAT_VERSION };

	private final CryptoComponent crypto;

	@Inject
    MessageFactoryImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public Message createMessage(GroupId g, long timestamp, byte[] body) {
		if (body.length == 0) throw new IllegalArgumentException();
		if (body.length > MAX_MESSAGE_BODY_LENGTH)
			throw new IllegalArgumentException();
		MessageId id = getMessageId(g, timestamp, body);
		return new Message(id, g, timestamp, body);
	}

	private MessageId getMessageId(GroupId g, long timestamp, byte[] body) {
		// There's only one block, so the root hash is the hash of the block
		byte[] rootHash = crypto.hash(BLOCK_LABEL, FORMAT_VERSION_BYTES, body);
		byte[] timeBytes = new byte[INT_64_BYTES];
		ByteUtils.writeUint64(timestamp, timeBytes, 0);
		byte[] idHash = crypto.hash(ID_LABEL, FORMAT_VERSION_BYTES,
				g.getBytes(), timeBytes, rootHash);
		return new MessageId(idHash);
	}

	@Override
	public Message createMessage(byte[] raw) {
		if (raw.length <= MESSAGE_HEADER_LENGTH)
			throw new IllegalArgumentException();
		if (raw.length > MAX_MESSAGE_LENGTH)
			throw new IllegalArgumentException();
		byte[] groupId = new byte[UniqueId.LENGTH];
		System.arraycopy(raw, 0, groupId, 0, UniqueId.LENGTH);
		GroupId g = new GroupId(groupId);
		long timestamp = ByteUtils.readUint64(raw, UniqueId.LENGTH);
		byte[] body = new byte[raw.length - MESSAGE_HEADER_LENGTH];
		System.arraycopy(raw, MESSAGE_HEADER_LENGTH, body, 0, body.length);
		MessageId id = getMessageId(g, timestamp, body);
		return new Message(id, g, timestamp, body);
	}

	@Override
	public byte[] getRawMessage(Message m) {
		byte[] body = m.getBody();
		byte[] raw = new byte[MESSAGE_HEADER_LENGTH + body.length];
		System.arraycopy(m.getGroupId().getBytes(), 0, raw, 0, UniqueId.LENGTH);
		ByteUtils.writeUint64(m.getTimestamp(), raw, UniqueId.LENGTH);
		System.arraycopy(body, 0, raw, MESSAGE_HEADER_LENGTH, body.length);
		return raw;
	}
}
