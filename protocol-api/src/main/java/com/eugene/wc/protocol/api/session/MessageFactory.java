package com.eugene.wc.protocol.api.session;

public interface MessageFactory {

	Message createMessage(GroupId g, long timestamp, byte[] body);

	Message createMessage(byte[] raw);

	byte[] getRawMessage(Message m);

//	Message createMessageTest(String data)
}
