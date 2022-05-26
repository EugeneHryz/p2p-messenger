package com.eugene.wc.protocol.api.sync;

public interface MessageFactory {

	Message createMessage(GroupId g, long timestamp, byte[] body);

	Message createMessage(byte[] raw);

	byte[] getRawMessage(Message m);
}
