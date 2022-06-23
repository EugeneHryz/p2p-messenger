package com.eugene.wc.protocol.client;

import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.data.MetadataEncoder;
import com.eugene.wc.protocol.api.data.MetadataParser;
import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.data.WdfReader;
import com.eugene.wc.protocol.api.data.WdfWriter;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityFactory;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageFactory;
import com.eugene.wc.protocol.api.session.MessageId;
import com.eugene.wc.protocol.api.session.Metadata;
import com.eugene.wc.protocol.data.WdfReaderImpl;
import com.eugene.wc.protocol.data.WdfWriterImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class ClientHelperImpl implements ClientHelper {

	private final DatabaseComponent db;
	private final MessageFactory messageFactory;
	private final MetadataParser metadataParser;
	private final MetadataEncoder metadataEncoder;
	private final IdentityFactory identityFactory;

	@Inject
    public ClientHelperImpl(DatabaseComponent db,
							MessageFactory messageFactory,
							MetadataParser metadataParser,
							MetadataEncoder metadataEncoder,
							IdentityFactory identityFactory) {
		this.db = db;
		this.messageFactory = messageFactory;
		this.metadataParser = metadataParser;
		this.metadataEncoder = metadataEncoder;
		this.identityFactory = identityFactory;
	}

	@Override
	public void addLocalMessage(Message m, WdfDictionary2 metadata,
								boolean shared) throws DbException, FormatException {
		db.runInTransaction(false, txn -> addLocalMessage(txn, m, metadata, shared,
				false));
	}

	@Override
	public void addLocalMessage(Connection txn, Message m, WdfDictionary2 metadata,
								boolean shared, boolean temporary) throws DbException, FormatException {
		db.addLocalMessage(txn, m, metadataEncoder.encode(metadata), shared, temporary);
	}

	@Override
	public Message createMessage(GroupId g, long timestamp, byte[] body) {
		return messageFactory.createMessage(g, timestamp, body);
	}

	@Override
	public Message createMessage(GroupId g, long timestamp, WdfList2 body) throws FormatException {
		return messageFactory.createMessage(g, timestamp, toByteArray(body));
	}

	@Override
	public WdfList2 getMessageAsList(MessageId m) throws DbException,
			FormatException {
		return db.runInTransactionWithResult(true, txn -> getMessageAsList(txn, m));
	}

	@Override
	public WdfList2 getMessageAsList(Connection txn, MessageId m)
			throws DbException, FormatException {
		return toList(db.getMessage(txn, m).getBody());
	}

	@Override
	public WdfDictionary2 getGroupMetadataAsDictionary(GroupId g)
			throws DbException, FormatException {
		return db.runInTransactionWithResult(true, txn ->
				getGroupMetadataAsDictionary(txn, g));
	}

	@Override
	public WdfDictionary2 getGroupMetadataAsDictionary(Connection txn,
			GroupId g) throws DbException, FormatException {
		Metadata metadata = db.getGroupMetadata(txn, g);
		return metadataParser.parse(metadata);
	}

	@Override
	public WdfDictionary2 getMessageMetadataAsDictionary(MessageId m)
			throws DbException, FormatException {
		return db.runInTransactionWithResult(true, txn ->
				getMessageMetadataAsDictionary(txn, m));
	}

	@Override
	public WdfDictionary2 getMessageMetadataAsDictionary(Connection txn,
			MessageId m) throws DbException, FormatException {
		Metadata metadata = db.getMessageMetadata(txn, m);
		return metadataParser.parse(metadata);
	}

	@Override
	public Map<MessageId, WdfDictionary2> getMessageMetadataAsDictionary(
			GroupId g) throws DbException, FormatException {
		return db.runInTransactionWithResult(true, txn ->
				getMessageMetadataAsDictionary(txn, g));
	}

	@Override
	public Map<MessageId, WdfDictionary2> getMessageMetadataAsDictionary(Connection txn, GroupId g)
			throws DbException, FormatException {

		Map<MessageId, Metadata> raw = db.getMessageMetadata(txn, g);
		Map<MessageId, WdfDictionary2> parsed = new HashMap<>(raw.size());
		for (Map.Entry<MessageId, Metadata> e : raw.entrySet())
			parsed.put(e.getKey(), metadataParser.parse(e.getValue()));
		return parsed;
	}
	@Override
	public void mergeGroupMetadata(GroupId g, WdfDictionary2 metadata)
			throws DbException, FormatException {
		db.runInTransaction(false, txn -> mergeGroupMetadata(txn, g, metadata));
	}

	@Override
	public void mergeGroupMetadata(Connection txn, GroupId g,
			WdfDictionary2 metadata) throws DbException, FormatException {
		db.mergeGroupMetadata(txn, g, metadataEncoder.encode(metadata));
	}

	@Override
	public void mergeMessageMetadata(MessageId m, WdfDictionary2 metadata)
			throws DbException, FormatException {
		db.runInTransaction(false, txn -> mergeMessageMetadata(txn, m, metadata));
	}

	@Override
	public void mergeMessageMetadata(Connection txn, MessageId m,
			WdfDictionary2 metadata) throws DbException, FormatException {
		db.mergeMessageMetadata(txn, m, metadataEncoder.encode(metadata));
	}

	@Override
	public byte[] toByteArray(WdfDictionary2 dictionary) throws FormatException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WdfWriter writer = new WdfWriterImpl(out);
		try {
			writer.writeDictionary(dictionary);
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	@Override
	public byte[] toByteArray(WdfList2 list) throws FormatException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WdfWriter writer = new WdfWriterImpl(out);
		try {
			writer.writeList(list);
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	@Override
	public WdfDictionary2 toDictionary(byte[] b, int off, int len)
			throws FormatException {
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		WdfReader reader = new WdfReaderImpl(in);
		try {
			WdfDictionary2 dictionary = reader.readDictionary();
			if (!reader.eof()) throw new FormatException();
			return dictionary;
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public WdfDictionary2 toDictionary(TransportProperties transportProperties) {
		return new WdfDictionary2(transportProperties);
	}

	@Override
	public WdfDictionary2 toDictionary(
			Map<TransportId, TransportProperties> map) {
		WdfDictionary2 d = new WdfDictionary2();
		for (Map.Entry<TransportId, TransportProperties> e : map.entrySet())
			d.put(e.getKey().toString(), new WdfDictionary2(e.getValue()));
		return d;
	}

	@Override
	public WdfList2 toList(byte[] b, int off, int len) throws FormatException {
		ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
		WdfReader reader = new WdfReaderImpl(in);
		try {
			WdfList2 list = reader.readList();
			if (!reader.eof()) throw new FormatException();
			return list;
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public WdfList2 toList(byte[] b) throws FormatException {
		return toList(b, 0, b.length);
	}

	@Override
	public WdfList2 toList(Identity identity) {
		return WdfList2.of(identity.getName(), identity.getPublicKey());
	}

	@Override
	public Identity parseIdentity(WdfList2 author) throws FormatException {
		String name = author.getString(0);
		PublicKey pubKey = new PublicKey(author.getRaw(1));

		return identityFactory.createIdentity(name, pubKey);
	}

	@Override
	public TransportProperties parseTransportProperties(WdfDictionary2 properties) throws FormatException {
		TransportProperties p = new TransportProperties();
		for (String key : properties.keySet()) {
			String value = properties.getString(key);
			p.put(key, value);
		}
		return p;
	}

	@Override
	public Map<TransportId, TransportProperties> parseTransportPropertiesMap(
			WdfDictionary2 properties) throws FormatException {
		Map<TransportId, TransportProperties> tpMap = new HashMap<>();
		for (String key : properties.keySet()) {
			TransportId transportId = new TransportId(key);
			TransportProperties transportProperties =
					parseTransportProperties(properties.getDictionary(key));
			tpMap.put(transportId, transportProperties);
		}
		return tpMap;
	}
}
