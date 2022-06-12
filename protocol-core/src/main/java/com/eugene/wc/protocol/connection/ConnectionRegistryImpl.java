package com.eugene.wc.protocol.connection;

import static java.util.Collections.emptyList;
import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

import com.eugene.wc.protocol.api.ByteArray;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.connection.Priority;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.plugin.PluginConfig;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.plugin.event.ConnectionClosedEvent;
import com.eugene.wc.protocol.api.plugin.event.ConnectionOpenedEvent;
import com.eugene.wc.protocol.api.plugin.event.ContactConnectedEvent;
import com.eugene.wc.protocol.api.plugin.event.ContactDisconnectedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

@ThreadSafe
class ConnectionRegistryImpl implements ConnectionRegistry {

	private static final Logger LOG =
			getLogger(ConnectionRegistryImpl.class.getName());

	private final EventBus eventBus;
	private final Map<TransportId, List<TransportId>> transportPrefs;

	private final Object lock = new Object();
	@GuardedBy("lock")
	private final Map<ContactId, List<ConnectionRecord>> contactConnections;

	@Inject
    ConnectionRegistryImpl(EventBus eventBus, PluginConfig pluginConfig) {
		this.eventBus = eventBus;
		transportPrefs = pluginConfig.getTransportPreferences();
		contactConnections = new HashMap<>();
	}

	@Override
	public void registerIncomingConnection(ContactId c, TransportId t,
			DuplexTransportConnection conn) {
		registerConnection(c, t, conn, true);
	}

	@Override
	public void registerOutgoingConnection(ContactId c, TransportId t,
			DuplexTransportConnection conn, Priority priority) {
		registerConnection(c, t, conn, false);
		setPriority(c, t, conn, priority);
	}

	private void registerConnection(ContactId c, TransportId t,
			DuplexTransportConnection conn, boolean incoming) {
		if (LOG.isLoggable(INFO)) {
			if (incoming) LOG.info("Incoming connection registered: " + t);
			else LOG.info("Outgoing connection registered: " + t);
		}
		boolean firstConnection;
		synchronized (lock) {
			List<ConnectionRecord> recs = contactConnections.get(c);
			if (recs == null) {
				recs = new ArrayList<>();
				contactConnections.put(c, recs);
			}
			firstConnection = recs.isEmpty();
			recs.add(new ConnectionRecord(t, conn, incoming));
		}
		eventBus.broadcast(new ConnectionOpenedEvent(c, t, incoming));
		if (firstConnection) {
			LOG.info("Contact connected");
			eventBus.broadcast(new ContactConnectedEvent(c));
		}
	}

	@Override
	public void setPriority(ContactId c, TransportId t,
			DuplexTransportConnection conn, Priority priority) {
		if (LOG.isLoggable(INFO)) LOG.info("Setting connection priority: " + t);
		List<DuplexTransportConnection> toInterrupt;
		boolean interruptNewConnection = false;
		synchronized (lock) {
			List<ConnectionRecord> recs = contactConnections.get(c);
			if (recs == null) throw new IllegalArgumentException();
			toInterrupt = new ArrayList<>(recs.size());
			for (ConnectionRecord rec : recs) {
				if (rec.conn == conn) {
					// Store the priority of this connection
					rec.priority = priority;
				} else if (rec.priority != null) {
					int compare = compareConnections(t, priority,
							rec.transportId, rec.priority);
					if (compare == -1) {
						// The old connection is better than the new one
						interruptNewConnection = true;
					} else if (compare == 1 && !rec.interrupted) {
						// The new connection is better than the old one
						toInterrupt.add(rec.conn);
						rec.interrupted = true;
					}
				}
			}
		}
		if (interruptNewConnection) {
			LOG.info("Interrupting new connection");
		}
	}

	private int compareConnections(TransportId tA, Priority pA, TransportId tB,
			Priority pB) {
		if (getBetterTransports(tA).contains(tB)) return -1;
		if (getBetterTransports(tB).contains(tA)) return 1;
		return tA.equals(tB) ? ByteArray.compare(pA.getNonce(), pB.getNonce()) : 0;
	}

	private List<TransportId> getBetterTransports(TransportId t) {
		List<TransportId> better = transportPrefs.get(t);
		return better == null ? emptyList() : better;
	}

	@Override
	public void unregisterConnection(ContactId c, TransportId t,
			DuplexTransportConnection conn, boolean incoming, boolean exception) {
		if (LOG.isLoggable(INFO)) {
			if (incoming) LOG.info("Incoming connection unregistered: " + t);
			else LOG.info("Outgoing connection unregistered: " + t);
		}
		boolean lastConnection;
		synchronized (lock) {
			List<ConnectionRecord> recs = contactConnections.get(c);
			if (recs == null || !recs.remove(new ConnectionRecord(t, conn, incoming)))
				throw new IllegalArgumentException();
			lastConnection = recs.isEmpty();
		}
		eventBus.broadcast(new ConnectionClosedEvent(c, t, incoming, exception));
		if (lastConnection) {
			LOG.info("Contact disconnected");
			eventBus.broadcast(new ContactDisconnectedEvent(c));
		}
	}

	@Override
	public Collection<ContactId> getConnectedContacts(TransportId t) {
		synchronized (lock) {
			List<ContactId> contactIds = new ArrayList<>();
			for (Map.Entry<ContactId, List<ConnectionRecord>> e :
					contactConnections.entrySet()) {
				for (ConnectionRecord rec : e.getValue()) {
					if (rec.transportId.equals(t)) {
						contactIds.add(e.getKey());
						break;
					}
				}
			}
			if (LOG.isLoggable(INFO)) {
				LOG.info(contactIds.size() + " contacts connected: " + t);
			}
			return contactIds;
		}
	}

	@Override
	public Collection<ContactId> getConnectedOrBetterContacts(TransportId t) {
		synchronized (lock) {
			List<TransportId> better = getBetterTransports(t);
			List<ContactId> contactIds = new ArrayList<>();
			for (Map.Entry<ContactId, List<ConnectionRecord>> e :
					contactConnections.entrySet()) {
				for (ConnectionRecord rec : e.getValue()) {
					if (rec.transportId.equals(t) ||
							better.contains(rec.transportId)) {
						contactIds.add(e.getKey());
						break;
					}
				}
			}
			if (LOG.isLoggable(INFO)) {
				LOG.info(contactIds.size()
						+ " contacts connected or better: " + t);
			}
			return contactIds;
		}
	}

	@Override
	public boolean isConnected(ContactId c, TransportId t) {
		synchronized (lock) {
			List<ConnectionRecord> recs = contactConnections.get(c);
			if (recs == null) return false;
			for (ConnectionRecord rec : recs) {
				if (rec.transportId.equals(t)) return true;
			}
			return false;
		}
	}

	@Override
	public boolean isConnected(ContactId c) {
		synchronized (lock) {
			List<ConnectionRecord> recs = contactConnections.get(c);
			return recs != null && !recs.isEmpty();
		}
	}

	public static class ConnectionRecord {

		private final TransportId transportId;
		private final DuplexTransportConnection conn;
		private final boolean incoming;
		@GuardedBy("lock")
		@Nullable
		private Priority priority = null;
		@GuardedBy("lock")
		private boolean interrupted = false;

		public ConnectionRecord(TransportId transportId,
				DuplexTransportConnection conn, boolean incoming) {
			this.transportId = transportId;
			this.conn = conn;
			this.incoming = incoming;
		}

		public TransportId getTransportId() {
			return transportId;
		}

		public DuplexTransportConnection getConnection() {
			return conn;
		}

		public boolean isIncoming() {
			return incoming;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ConnectionRecord) {
				return conn == ((ConnectionRecord) o).conn;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return conn.hashCode();
		}
	}
}
