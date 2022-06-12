package com.eugene.wc.protocol.api.client;

import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageId;

import java.sql.Connection;
import java.util.Map;

public interface ClientHelper {

	void addLocalMessage(Message m, WdfDictionary2 metadata, boolean shared)
			throws DbException, FormatException;

	void addLocalMessage(Connection txn, Message m, WdfDictionary2 metadata,
						 boolean shared, boolean temporary)
			throws DbException, FormatException;

	Message createMessage(GroupId g, long timestamp, byte[] body);

	Message createMessage(GroupId g, long timestamp, WdfList2 body)
			throws FormatException;

	WdfList2 getMessageAsList(MessageId m) throws DbException, FormatException;

	WdfList2 getMessageAsList(Connection txn, MessageId m) throws DbException,
			FormatException;

	WdfDictionary2 getGroupMetadataAsDictionary(GroupId g) throws DbException,
			FormatException;

	WdfDictionary2 getGroupMetadataAsDictionary(Connection txn, GroupId g)
			throws DbException, FormatException;

	WdfDictionary2 toDictionary(byte[] b, int off, int len) throws FormatException;

	WdfDictionary2 toDictionary(TransportProperties transportProperties);

	WdfDictionary2 toDictionary(Map<TransportId, TransportProperties> map);

	WdfList2 toList(byte[] b, int off, int len) throws FormatException;

	WdfList2 toList(byte[] b) throws FormatException;

	WdfList2 toList(Identity identity);

	WdfDictionary2 getMessageMetadataAsDictionary(MessageId m)
			throws DbException, FormatException;

	WdfDictionary2 getMessageMetadataAsDictionary(Connection txn, MessageId m)
			throws DbException, FormatException;

	Map<MessageId, WdfDictionary2> getMessageMetadataAsDictionary(GroupId g)
			throws DbException, FormatException;

	Map<MessageId, WdfDictionary2> getMessageMetadataAsDictionary(
			Connection txn, GroupId g) throws DbException, FormatException;

	void mergeGroupMetadata(GroupId g, WdfDictionary2 metadata)
			throws DbException, FormatException;

	void mergeGroupMetadata(Connection txn, GroupId g, WdfDictionary2 metadata)
			throws DbException, FormatException;

	void mergeMessageMetadata(MessageId m, WdfDictionary2 metadata)
			throws DbException, FormatException;

	void mergeMessageMetadata(Connection txn, MessageId m,
			WdfDictionary2 metadata) throws DbException, FormatException;

	byte[] toByteArray(WdfDictionary2 dictionary) throws FormatException;

	byte[] toByteArray(WdfList2 list) throws FormatException;

	Identity parseIdentity(WdfList2 author) throws FormatException;

	TransportProperties parseTransportProperties(WdfDictionary2 properties) throws FormatException;

	Map<TransportId, TransportProperties> parseTransportPropertiesMap(
			WdfDictionary2 properties) throws FormatException;

}
