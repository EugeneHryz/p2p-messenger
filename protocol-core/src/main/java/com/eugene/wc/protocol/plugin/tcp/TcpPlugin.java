package com.eugene.wc.protocol.plugin.tcp;

import static com.eugene.wc.protocol.api.plugin.Plugin.State.ACTIVE;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.DISABLED;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.INACTIVE;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.STARTING_STOPPING;
import static com.eugene.wc.protocol.api.util.IoUtils.tryToClose;
import static com.eugene.wc.protocol.api.util.NetworkUtils.getNetworkInterfaces;
import static com.eugene.wc.protocol.api.util.PrivacyUtils.scrubSocketAddress;
import static com.eugene.wc.protocol.api.util.StringUtils.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

import com.eugene.wc.protocol.PoliteExecutor;
import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.plugin.Backoff;
import com.eugene.wc.protocol.api.plugin.ConnectionHandler;
import com.eugene.wc.protocol.api.plugin.PluginCallback;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.api.settings.event.SettingsUpdatedEvent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

abstract class TcpPlugin implements DuplexPlugin, EventListener {

	private static final Logger logger = getLogger(TcpPlugin.class.getName());

	private static final Pattern DOTTED_QUAD =
			Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

	protected final Executor ioExecutor, wakefulIoExecutor, bindExecutor;
	protected final Backoff backoff;
	protected final PluginCallback callback;
	protected final long maxLatency;
	protected final int maxIdleTime, connectionTimeout, socketTimeout;
	protected final AtomicBoolean used = new AtomicBoolean(false);
	protected final PluginState state = new PluginState();

	/**
	 * Returns zero or more socket addresses on which the plugin should listen,
	 * in order of preference. At most one of the addresses will be bound.
	 */
	protected abstract List<InetSocketAddress> getLocalSocketAddresses(
			boolean ipv4);

	/**
	 * Adds the address on which the plugin is listening to the transport
	 * properties.
	 */
	protected abstract void setLocalSocketAddress(InetSocketAddress a,
			boolean ipv4);

	/**
	 * Returns zero or more socket addresses for connecting to a contact with
	 * the given transport properties.
	 */
	protected abstract List<InetSocketAddress> getRemoteSocketAddresses(TransportProperties p, boolean ipv4);

	/**
	 * Returns true if connections to the given address can be attempted.
	 */
	protected abstract boolean isConnectable(InterfaceAddress local, InetSocketAddress remote);

	/**
	 * Returns true if the plugin is enabled by default.
	 */
	protected abstract boolean isEnabledByDefault();

	TcpPlugin(Executor ioExecutor,
			Executor wakefulIoExecutor,
			Backoff backoff,
			PluginCallback callback,
			long maxLatency,
			int maxIdleTime,
			int connectionTimeout) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.backoff = backoff;
		this.callback = callback;
		this.maxLatency = maxLatency;
		this.maxIdleTime = maxIdleTime;
		this.connectionTimeout = connectionTimeout;
		if (maxIdleTime > Integer.MAX_VALUE / 2)
			socketTimeout = Integer.MAX_VALUE;
		else socketTimeout = maxIdleTime * 2;
		// Don't execute more than one bind operation at a time
		bindExecutor = new PoliteExecutor("TcpPlugin", ioExecutor, 1);
	}

	@Override
	public long getMaxLatency() {
		return maxLatency;
	}

	@Override
	public int getMaxIdleTime() {
		return maxIdleTime;
	}

	@Override
	public void start() {
		if (used.getAndSet(true)) throw new IllegalStateException();
		Settings settings = callback.getSettings();
		state.setStarted(
				settings.getBoolean(PREF_PLUGIN_ENABLE, isEnabledByDefault()));
		bind();
	}

	protected void bind() {
		bindExecutor.execute(() -> {
			State s = getState();
			if (s != ACTIVE && s != INACTIVE) return;
			bind(true);
			bind(false);
		});
	}

	private void bind(boolean ipv4) {
		ServerSocket old = state.getServerSocket(ipv4);
		ServerSocket ss = null;
		for (InetSocketAddress addr : getLocalSocketAddresses(ipv4)) {
			if (old != null && addr.equals(old.getLocalSocketAddress())) {
				logger.info("Server socket already bound");
				return;
			}
			try {
				ss = new ServerSocket();
				ss.bind(addr);
				break;
			} catch (IOException e) {
				if (logger.isLoggable(INFO))
					logger.info("Failed to bind " + scrubSocketAddress(addr));
				tryToClose(ss, logger, WARNING);
			}
		}
		if (ss == null || !ss.isBound()) {
			logger.info("Could not bind server socket");
			return;
		}
		if (!state.setServerSocket(ss, ipv4)) {
			logger.info("Closing redundant server socket");
			tryToClose(ss, logger, WARNING);
			return;
		}
		backoff.reset();
		InetSocketAddress local =
				(InetSocketAddress) ss.getLocalSocketAddress();
		setLocalSocketAddress(local, ipv4);
		if (logger.isLoggable(INFO))
			logger.info("Listening on " + scrubSocketAddress(local));
		ServerSocket finalSocket = ss;
		ioExecutor.execute(() -> acceptContactConnections(finalSocket, ipv4));
	}

	public String getIpPortString(InetSocketAddress a) {
		String addr = a.getAddress().getHostAddress();
		int percent = addr.indexOf('%');
		if (percent != -1) addr = addr.substring(0, percent);
		return addr + ":" + a.getPort();
	}

	private void acceptContactConnections(ServerSocket ss, boolean ipv4) {
		while (true) {
			Socket s;
			try {
				s = ss.accept();
				s.setSoTimeout(socketTimeout);
			} catch (IOException e) {
				// This is expected when the server socket is closed
				logger.info("Server socket closed");
				state.clearServerSocket(ss, ipv4);
				return;
			}
			if (logger.isLoggable(INFO)) {
				logger.info("Connection from " +
						scrubSocketAddress(s.getRemoteSocketAddress()));
			}
			backoff.reset();
			callback.handleConnection(new TcpTransportConnection(this, s));
		}
	}

	@Override
	public void stop() {
		for (ServerSocket ss : state.setStopped()) tryToClose(ss, logger, WARNING);
	}

	@Override
	public State getState() {
		return state.getState();
	}

	@Override
	public int getReasonsDisabled() {
		return state.getReasonsDisabled();
	}

	@Override
	public boolean shouldPoll() {
		return true;
	}

	@Override
	public int getPollingInterval() {
		return backoff.getPollingInterval();
	}

	@Override
	public void poll(Collection<Pair<TransportProperties, ConnectionHandler>>
			properties) {
		if (getState() != ACTIVE) return;
		backoff.increment();
		for (Pair<TransportProperties, ConnectionHandler> p : properties) {
			connect(p.getFirst(), p.getSecond());
		}
	}

	private void connect(TransportProperties p, ConnectionHandler h) {
		wakefulIoExecutor.execute(() -> {
			DuplexTransportConnection d = createConnection(p);
			if (d != null) {
				backoff.reset();
				h.handleConnection(d);
			}
		});
	}

	@Override
	public DuplexTransportConnection createConnection(TransportProperties p) {
		DuplexTransportConnection c = createConnection(p, true);
		if (c != null) return c;
		return createConnection(p, false);
	}

	@Nullable
	private DuplexTransportConnection createConnection(TransportProperties p,
			boolean ipv4) {
		ServerSocket ss = state.getServerSocket(ipv4);
		if (ss == null) return null;
		InterfaceAddress local = getLocalInterfaceAddress(ss.getInetAddress());
		if (local == null) {
			logger.warning("No interface for server socket");
			return null;
		}
		for (InetSocketAddress remote : getRemoteSocketAddresses(p, ipv4)) {
			// Don't try to connect to our own address
			if (!canConnectToOwnAddress() &&
					remote.getAddress().equals(ss.getInetAddress())) {
				continue;
			}
			if (!isConnectable(local, remote)) {
				if (logger.isLoggable(INFO)) {
					logger.info(scrubSocketAddress(remote) +
							" is not connectable from " +
							scrubSocketAddress(ss.getLocalSocketAddress()));
				}
				continue;
			}
			try {
				if (logger.isLoggable(INFO))
					logger.info("Connecting to " + scrubSocketAddress(remote));
				Socket s = createSocket();
				s.bind(new InetSocketAddress(ss.getInetAddress(), 0));
				s.connect(remote, connectionTimeout);
				s.setSoTimeout(socketTimeout);
				if (logger.isLoggable(INFO))
					logger.info("Connected to " + scrubSocketAddress(remote));
				return new TcpTransportConnection(this, s);
			} catch (IOException e) {
				if (logger.isLoggable(INFO)) {
					logger.info("Could not connect to " +
							scrubSocketAddress(remote));
				}
			}
		}
		return null;
	}

	@Nullable
	public InterfaceAddress getLocalInterfaceAddress(InetAddress a) {
		for (InterfaceAddress ifAddr : getLocalInterfaceAddresses()) {
			if (ifAddr.getAddress().equals(a)) return ifAddr;
		}
		return null;
	}

	// Override for testing
	protected boolean canConnectToOwnAddress() {
		return false;
	}

	protected Socket createSocket() throws IOException {
		return new Socket();
	}

	public int chooseEphemeralPort() {
		return 32768 + (int) (Math.random() * 32768);
	}

	@Nullable
	public InetSocketAddress parseIpv4SocketAddress(String ipPort) {
		if (isNullOrEmpty(ipPort)) return null;
		String[] split = ipPort.split(":");
		if (split.length != 2) return null;
		String addr = split[0], port = split[1];
		// Ensure getByName() won't perform a DNS lookup
		if (!DOTTED_QUAD.matcher(addr).matches()) return null;
		try {
			InetAddress a = InetAddress.getByName(addr);
			int p = Integer.parseInt(port);
			return new InetSocketAddress(a, p);
		} catch (UnknownHostException | NumberFormatException e) {
			return null;
		}
	}

	@Override
	public boolean supportsKeyAgreement() {
		return false;
	}

//	@Override
//	public boolean supportsRendezvous() {
//		return false;
//	}

	public List<InterfaceAddress> getLocalInterfaceAddresses() {
		List<InterfaceAddress> addrs = new ArrayList<>();
		for (NetworkInterface iface : getNetworkInterfaces()) {
			addrs.addAll(iface.getInterfaceAddresses());
		}
		return addrs;
	}

	public List<InetAddress> getLocalInetAddresses() {
		List<InetAddress> addrs = new ArrayList<>();
		for (NetworkInterface iface : getNetworkInterfaces()) {
			addrs.addAll(list(iface.getInetAddresses()));
		}
		return addrs;
	}

	@Override
	public void onEventOccurred(Event e) {
		if (e instanceof SettingsUpdatedEvent) {
			SettingsUpdatedEvent s = (SettingsUpdatedEvent) e;
			if (s.getNamespace().equals(getId().toString()))
				ioExecutor.execute(() -> onSettingsUpdated(s.getSettings()));
		}
	}

	@IoExecutor
	private void onSettingsUpdated(Settings settings) {
		boolean enabledByUser =
				settings.getBoolean(PREF_PLUGIN_ENABLE, isEnabledByDefault());
		List<ServerSocket> toClose = state.setEnabledByUser(enabledByUser);
		State s = getState();
		if (!toClose.isEmpty()) {
			logger.info("Disabled by user, closing server sockets");
			for (ServerSocket ss : toClose) tryToClose(ss, logger, WARNING);
		} else if (s == INACTIVE) {
			logger.info("Enabled by user, opening server sockets");
			bind();
		}
	}

	@ThreadSafe
	protected class PluginState {

		@GuardedBy("this")
		private boolean started = false, stopped = false, enabledByUser = false;

		@GuardedBy("this")
		@Nullable
		private ServerSocket serverSocketV4 = null, serverSocketV6 = null;

		public synchronized void setStarted(boolean enabledByUser) {
			started = true;
			this.enabledByUser = enabledByUser;
			callback.pluginStateChanged(getState());
		}

		public synchronized List<ServerSocket> setStopped() {
			stopped = true;
			List<ServerSocket> toClose = clearServerSockets();
			callback.pluginStateChanged(getState());
			return toClose;
		}

		@GuardedBy("this")
		private List<ServerSocket> clearServerSockets() {
			List<ServerSocket> toClose = new ArrayList<>(2);
			if (serverSocketV4 != null) {
				toClose.add(serverSocketV4);
				serverSocketV4 = null;
			}
			if (serverSocketV6 != null) {
				toClose.add(serverSocketV6);
				serverSocketV6 = null;
			}
			return toClose;
		}

		public synchronized List<ServerSocket> setEnabledByUser(
				boolean enabledByUser) {
			this.enabledByUser = enabledByUser;
			List<ServerSocket> toClose = enabledByUser
					? emptyList() : clearServerSockets();
			callback.pluginStateChanged(getState());
			return toClose;
		}

		@Nullable
		public synchronized ServerSocket getServerSocket(boolean ipv4) {
			return ipv4 ? serverSocketV4 : serverSocketV6;
		}

		public synchronized boolean setServerSocket(ServerSocket ss, boolean ipv4) {
			if (stopped) return false;
			if (ipv4) {
				if (serverSocketV4 != null) return false;
				serverSocketV4 = ss;
			} else {
				if (serverSocketV6 != null) return false;
				serverSocketV6 = ss;
			}
			callback.pluginStateChanged(getState());
			return true;
		}

		public synchronized void clearServerSocket(ServerSocket ss, boolean ipv4) {
			if (ipv4) {
				if (serverSocketV4 == ss) serverSocketV4 = null;
			} else {
				if (serverSocketV6 == ss) serverSocketV6 = null;
			}
			callback.pluginStateChanged(getState());
		}

		public synchronized State getState() {
			if (!started || stopped) return STARTING_STOPPING;
			if (!enabledByUser) return DISABLED;
			if (serverSocketV4 != null || serverSocketV6 != null) return ACTIVE;
			return INACTIVE;
		}

		public synchronized int getReasonsDisabled() {
			return getState() == DISABLED ? REASON_USER : 0;
		}
	}
}
