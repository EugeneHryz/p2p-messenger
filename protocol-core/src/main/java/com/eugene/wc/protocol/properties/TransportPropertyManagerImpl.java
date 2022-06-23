package com.eugene.wc.protocol.properties;

import static com.eugene.wc.protocol.api.properties.TransportPropertyConstants.*;
import static com.eugene.wc.protocol.api.util.StringUtils.isNullOrEmpty;

import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;
import com.eugene.wc.protocol.api.contact.Contact;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.data.WdfDictionary2;
import com.eugene.wc.protocol.api.data.WdfList2;
import com.eugene.wc.protocol.api.data.exception.FormatException;
import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.DatabaseOpenListener;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.api.session.Group;
import com.eugene.wc.protocol.api.session.GroupId;
import com.eugene.wc.protocol.api.session.Message;
import com.eugene.wc.protocol.api.session.MessageId;
import com.eugene.wc.protocol.api.system.Clock;
import com.eugene.wc.protocol.api.util.StringUtils;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class TransportPropertyManagerImpl implements TransportPropertyManager, DatabaseOpenListener,
		ContactManager.ContactHook {

	private static final Logger logger = Logger.getLogger(TransportPropertyManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final ClientHelper clientHelper;
	private final ContactGroupFactory contactGroupFactory;
	private final Clock clock;
	private final Group localGroup;

	@Inject
	public TransportPropertyManagerImpl(DatabaseComponent db,
			ClientHelper clientHelper,
			ContactGroupFactory contactGroupFactory, Clock clock) {
		this.db = db;
		this.clientHelper = clientHelper;
		this.contactGroupFactory = contactGroupFactory;
		this.clock = clock;

		localGroup = contactGroupFactory.createLocalGroup(CLIENT_ID);
	}

	@Override
	public void onDatabaseOpened(Connection txn) {
		try {
			if (db.containsGroup(txn, localGroup.getId())) return;
			db.addGroup(txn, localGroup);

			for (Contact c : db.getAllContacts(txn)) addingContact(txn, c);
		} catch (DbException e) {
			logger.warning("Exception when calling onDatabaseOpened\n" + e);
		}
	}

	@Override
	public void addingContact(Connection txn, Contact c) throws DbException {
		Group g = getContactGroup(c);
		db.addGroup(txn, g);

		Map<TransportId, TransportProperties> local = getLocalProperties(txn);
		for (Entry<TransportId, TransportProperties> e : local.entrySet()) {
			storeMessage(txn, g.getId(), e.getKey(), e.getValue(), 1, true, true);
		}
	}

	@Override
	public void removingContact(Connection txn, Contact c) throws DbException {
		db.removeGroup(txn, getContactGroup(c));
	}

	@Override
	public void addRemoteProperties(Connection txn, ContactId c, Map<TransportId, TransportProperties> props)
			throws DbException {

		Group g = getContactGroup(db.getContactById(txn, c));
		for (Entry<TransportId, TransportProperties> e : props.entrySet()) {
			storeMessage(txn, g.getId(), e.getKey(), e.getValue(), 0, false, false);
		}
	}

	@Override
	public void addRemotePropertiesFromConnection(ContactId c, TransportId t, TransportProperties props)
			throws DbException {
		if (props.isEmpty()) return;
		try {
			db.runInTransaction(false, txn -> {
				Contact contact = db.getContactById(txn, c);
				Group g = getContactGroup(contact);
				WdfDictionary2 meta = clientHelper.getGroupMetadataAsDictionary(txn, g.getId());
				WdfDictionary2 discovered = meta.getOptionalDictionary(GROUP_KEY_DISCOVERED);
				WdfDictionary2 merged;
				boolean changed;
				if (discovered == null) {
					merged = new WdfDictionary2(props);
					changed = true;
				} else {
					merged = new WdfDictionary2(discovered);
					merged.putAll(props);
					changed = !merged.equals(discovered);
				}
				if (changed) {
					meta.put(GROUP_KEY_DISCOVERED, merged);
					clientHelper.mergeGroupMetadata(txn, g.getId(), meta);
					updateLocalProperties(txn, contact, t);
				}
			});
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public Map<TransportId, TransportProperties> getLocalProperties() throws DbException {
		return db.runInTransactionWithResult(true, this::getLocalProperties);
	}

	@Override
	public Map<TransportId, TransportProperties> getLocalProperties(Connection txn) throws DbException {
		try {
			Map<TransportId, TransportProperties> local = new HashMap<>();
			// Find the latest local update for each transport
			Map<TransportId, LatestUpdate> latest = findLatestLocal(txn);
			// Retrieve and parse the latest local properties
			for (Entry<TransportId, LatestUpdate> e : latest.entrySet()) {
				WdfList2 message = clientHelper.getMessageAsList(txn, e.getValue().messageId);
				local.put(e.getKey(), parseProperties(message));
			}
			return local;
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public TransportProperties getLocalProperties(TransportId t)
			throws DbException {
		try {
			return db.runInTransactionWithResult(true, txn -> {
				TransportProperties p = null;
				// Find the latest local update
				LatestUpdate latest = findLatest(txn, localGroup.getId(), t,
						true);
				if (latest != null) {
					// Retrieve and parse the latest local properties
					WdfList2 message = clientHelper.getMessageAsList(txn, latest.messageId);
					p = parseProperties(message);
				}
				return p == null ? new TransportProperties() : p;
			});
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public Map<ContactId, TransportProperties> getRemoteProperties(TransportId t) throws DbException {
		return db.runInTransactionWithResult(true, txn -> {
			Map<ContactId, TransportProperties> remote = new HashMap<>();
			for (Contact c : db.getAllContacts(txn))
				remote.put(c.getId(), getRemoteProperties(txn, c, t));
			return remote;
		});
	}

	private void updateLocalProperties(Connection txn, Contact c, TransportId t) throws DbException {
		try {
			TransportProperties local;
			LatestUpdate latest = findLatest(txn, localGroup.getId(), t, true);
			if (latest == null) {
				local = new TransportProperties();
			} else {
				WdfList2 message = clientHelper.getMessageAsList(txn,
						latest.messageId);
				local = parseProperties(message);
			}
			storeLocalProperties(txn, c, t, local);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	private TransportProperties getRemoteProperties(Connection txn, Contact c,
													TransportId t) throws DbException {
		Group g = getContactGroup(c);
		try {
			// Find the latest remote update
			TransportProperties remote;
			LatestUpdate latest = findLatest(txn, g.getId(), t, false);
			if (latest == null) {
				remote = new TransportProperties();
			} else {
				// Retrieve and parse the latest remote properties
				WdfList2 message = clientHelper.getMessageAsList(txn, latest.messageId);
				remote = parseProperties(message);
			}
			// Merge in any discovered properties
			WdfDictionary2 meta = clientHelper.getGroupMetadataAsDictionary(txn, g.getId());
			WdfDictionary2 d = meta.getOptionalDictionary(GROUP_KEY_DISCOVERED);
			if (d == null) return remote;
			TransportProperties merged = clientHelper.parseTransportProperties(d);
			// Received properties override discovered properties
			merged.putAll(remote);
			return merged;
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public TransportProperties getRemoteProperties(ContactId c, TransportId t)
			throws DbException {
		return db.runInTransactionWithResult(true, txn ->
				getRemoteProperties(txn, db.getContactById(txn, c), t));
	}

	@Override
	public void mergeLocalProperties(TransportId t, TransportProperties p) throws DbException {
		try {
			db.runInTransaction(false, txn -> {
				// Merge the new properties with any existing properties
				TransportProperties merged;
				boolean changed;
				LatestUpdate latest = findLatest(txn, localGroup.getId(), t, true);
				if (latest == null) {
					merged = new TransportProperties(p);
					merged.values().removeIf(StringUtils::isNullOrEmpty);
					changed = true;
				} else {
					WdfList2 message = clientHelper.getMessageAsList(txn, latest.messageId);
					TransportProperties old = parseProperties(message);
					merged = new TransportProperties(old);

					for (Entry<String, String> e : p.entrySet()) {
						String key = e.getKey(), value = e.getValue();
						if (isNullOrEmpty(value)) merged.remove(key);
						else merged.put(key, value);
					}
					changed = !merged.equals(old);
				}
				if (changed) {
					// Store the merged properties in the local group
					long version = latest == null ? 1 : latest.version + 1;
					storeMessage(txn, localGroup.getId(), t, merged, version, true, false);
					// Delete the previous update, if any
					if (latest != null) db.removeMessage(txn, latest.messageId);

					// Store the merged properties in each contact's group
					for (Contact c : db.getAllContacts(txn)) {
						storeLocalProperties(txn, c, t, merged);
					}
				}
			});
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	private void storeLocalProperties(Connection txn, Contact c, TransportId t, TransportProperties p)
			throws DbException, FormatException {
		Group g = getContactGroup(c);
		LatestUpdate latest = findLatest(txn, g.getId(), t, true);
		long version = latest == null ? 1 : latest.version + 1;
		// Reflect any remote properties we've discovered
		WdfDictionary2 meta = clientHelper.getGroupMetadataAsDictionary(txn, g.getId());
		WdfDictionary2 discovered = meta.getOptionalDictionary(GROUP_KEY_DISCOVERED);
		TransportProperties combined;
		if (discovered == null) {
			combined = p;
		} else {
			combined = new TransportProperties(p);
			TransportProperties d = clientHelper.parseTransportProperties(discovered);
			for (Entry<String, String> e : d.entrySet()) {
				String key = REFLECTED_PROPERTY_PREFIX + e.getKey();
				combined.put(key, e.getValue());
			}
		}
		storeMessage(txn, g.getId(), t, combined, version, true, true);
		// Delete the previous update, if any
		if (latest != null) db.removeMessage(txn, latest.messageId);
	}

	private Group getContactGroup(Contact c) {
		return contactGroupFactory.createContactGroup(CLIENT_ID, c);
	}

	private void storeMessage(Connection txn, GroupId g, TransportId t,
			TransportProperties p, long version, boolean local, boolean shared)
			throws DbException {
		try {
			WdfList2 body = WdfList2.of(t.toString(), version, p);
			long now = clock.currentTimeMillis();
			Message m = clientHelper.createMessage(g, now, body);
			WdfDictionary2 meta = new WdfDictionary2();
			meta.put(MSG_KEY_TRANSPORT_ID, t.toString());
			meta.put(MSG_KEY_VERSION, version);
			meta.put(MSG_KEY_LOCAL, local);
			clientHelper.addLocalMessage(txn, m, meta, shared, false);
		} catch (FormatException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<TransportId, LatestUpdate> findLatestLocal(Connection txn) throws DbException, FormatException {
		Map<TransportId, LatestUpdate> latestUpdates = new HashMap<>();
		Map<MessageId, WdfDictionary2> metadata = clientHelper.getMessageMetadataAsDictionary(
				txn, localGroup.getId());

		for (Entry<MessageId, WdfDictionary2> e : metadata.entrySet()) {
			WdfDictionary2 meta = e.getValue();
			TransportId t = new TransportId(meta.getString(MSG_KEY_TRANSPORT_ID));
			long version = meta.getLong(MSG_KEY_VERSION);
			latestUpdates.put(t, new LatestUpdate(e.getKey(), version));
		}
		return latestUpdates;
	}

	@Nullable
	private LatestUpdate findLatest(Connection txn, GroupId g, TransportId t, boolean local)
			throws DbException, FormatException {

		Map<MessageId, WdfDictionary2> metadata = clientHelper.getMessageMetadataAsDictionary(txn, g);
		for (Entry<MessageId, WdfDictionary2> e : metadata.entrySet()) {
			WdfDictionary2 meta = e.getValue();
			if (meta.getString(MSG_KEY_TRANSPORT_ID).equals(t.toString())
					&& meta.getBoolean(MSG_KEY_LOCAL) == local) {

				return new LatestUpdate(e.getKey(), meta.getLong(MSG_KEY_VERSION));
			}
		}
		return null;
	}

	private TransportProperties parseProperties(WdfList2 message) throws FormatException {
		WdfDictionary2 dictionary = message.getDictionary(2);
		return clientHelper.parseTransportProperties(dictionary);
	}

	private static class LatestUpdate {

		private final MessageId messageId;
		private final long version;

		private LatestUpdate(MessageId messageId, long version) {
			this.messageId = messageId;
			this.version = version;
		}
	}
}
