package com.eugene.wc.plugin.tcp;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static com.eugene.wc.protocol.api.plugin.LanTcpConstants.DEFAULT_PREF_PLUGIN_ENABLE;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.ACTIVE;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.INACTIVE;
import static com.eugene.wc.protocol.api.util.IoUtils.tryToClose;
import static com.eugene.wc.protocol.api.util.LogUtils.logException;
import static com.eugene.wc.protocol.api.util.NetworkUtils.getNetworkInterfaces;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

import android.annotation.TargetApi;
import android.app.Application;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.eugene.wc.protocol.PoliteExecutor;
import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.network.event.NetworkStatusEvent;
import com.eugene.wc.protocol.api.plugin.Backoff;
import com.eugene.wc.protocol.api.plugin.PluginCallback;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.plugin.tcp.LanTcpPlugin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.SocketFactory;

public class AndroidLanTcpPlugin extends LanTcpPlugin {

	private static final Logger logger = getLogger(AndroidLanTcpPlugin.class.getName());

	/**
	 * The interface name is used as a heuristic for deciding whether the
	 * device is providing a wifi access point.
	 */
	private static final Pattern AP_INTERFACE_NAME =
			Pattern.compile("^(wlan|ap|p2p)[-0-9]");

	private final Executor connectionStatusExecutor;
	private final ConnectivityManager connectivityManager;
	private final WifiManager wifiManager;

	private volatile SocketFactory socketFactory;

	public AndroidLanTcpPlugin(Executor ioExecutor,
							   Executor wakefulIoExecutor,
							   Application app,
							   Backoff backoff,
							   PluginCallback callback,
							   long maxLatency,
							   int maxIdleTime,
							   int connectionTimeout) {
		super(ioExecutor, wakefulIoExecutor, backoff, callback, maxLatency,
				maxIdleTime, connectionTimeout);
		// Don't execute more than one connection status check at a time
		connectionStatusExecutor =
				new PoliteExecutor("AndroidLanTcpPlugin", ioExecutor, 1);
		connectivityManager = (ConnectivityManager)
				requireNonNull(app.getSystemService(CONNECTIVITY_SERVICE));
		wifiManager = (WifiManager) app.getSystemService(WIFI_SERVICE);
		socketFactory = SocketFactory.getDefault();
	}

	@Override
	public void start() {
		if (used.getAndSet(true)) throw new IllegalStateException();
		initialisePortProperty();
		Settings settings = callback.getSettings();
		state.setStarted(settings.getBoolean(PREF_PLUGIN_ENABLE,
				DEFAULT_PREF_PLUGIN_ENABLE));
		updateConnectionStatus();
	}

	@Override
	protected Socket createSocket() throws IOException {
		return socketFactory.createSocket();
	}

	@Override
	protected List<InetAddress> getUsableLocalInetAddresses(boolean ipv4) {
		InetAddress addr = getWifiAddress(ipv4);
		return addr == null ? emptyList() : singletonList(addr);
	}

	private InetAddress getWifiAddress(boolean ipv4) {
		Pair<InetAddress, Boolean> wifi = getWifiIpv4Address();
		if (ipv4) return wifi == null ? null : wifi.getFirst();
		// If there's no wifi IPv4 address, we might be a client on an
		// IPv6-only wifi network. We can only detect this on API 21+
		if (wifi == null) {
			return getWifiClientIpv6Address();
		}
		// Use the wifi IPv4 address to determine which interface's IPv6
		// address we should return (if the interface has a suitable address)
		return getIpv6AddressForInterface(wifi.getFirst());
	}

	/**
	 * Returns a {@link Pair} where the first element is the IPv4 address of
	 * the wifi interface and the second element is true if this device is
	 * providing an access point, or false if this device is a client. Returns
	 * null if this device isn't connected to wifi as an access point or client.
	 */
	private Pair<InetAddress, Boolean> getWifiIpv4Address() {
		if (wifiManager == null) return null;
		// If we're connected to a wifi network, return its address
		WifiInfo info = wifiManager.getConnectionInfo();
		if (info != null && info.getIpAddress() != 0) {
			return new Pair<>(intToInetAddress(info.getIpAddress()), false);
		}
		// If we're providing an access point, return its address
		for (NetworkInterface iface : getNetworkInterfaces()) {
			if (AP_INTERFACE_NAME.matcher(iface.getName()).find()) {
				for (InterfaceAddress ifAddr : iface.getInterfaceAddresses()) {
					if (isPossibleWifiApInterface(ifAddr)) {
						return new Pair<>(ifAddr.getAddress(), true);
					}
				}
			}
		}
		// Not connected to wifi
		return null;
	}

	/**
	 * Returns true if the given address may belong to an interface providing
	 * a wifi access point (including wifi direct legacy mode access points).
	 * <p>
	 * This method may return true for wifi client interfaces as well, but
	 * we've already checked for a wifi client connection above.
	 */
	private boolean isPossibleWifiApInterface(InterfaceAddress ifAddr) {
		if (ifAddr.getNetworkPrefixLength() != 24) return false;
		byte[] ip = ifAddr.getAddress().getAddress();
		return ip.length == 4
				&& ip[0] == (byte) 192
				&& ip[1] == (byte) 168;
	}

	/**
	 * Returns a link-local IPv6 address for the wifi client interface, or null
	 * if there's no such interface or it doesn't have a suitable address.
	 */
	@TargetApi(21)
	private InetAddress getWifiClientIpv6Address() {
		for (Network net : connectivityManager.getAllNetworks()) {
			NetworkCapabilities caps =
					connectivityManager.getNetworkCapabilities(net);
			if (caps == null || !caps.hasTransport(TRANSPORT_WIFI)) continue;
			LinkProperties props = connectivityManager.getLinkProperties(net);
			if (props == null) continue;
			for (LinkAddress linkAddress : props.getLinkAddresses()) {
				InetAddress addr = linkAddress.getAddress();
				if (isIpv6LinkLocalAddress(addr)) return addr;
			}
		}
		return null;
	}

	/**
	 * Returns a link-local IPv6 address for the interface with the given IPv4
	 * address, or null if the interface doesn't have a suitable address.
	 */
	private InetAddress getIpv6AddressForInterface(InetAddress ipv4) {
		try {
			NetworkInterface iface = NetworkInterface.getByInetAddress(ipv4);
			if (iface == null) return null;
			for (InetAddress addr : list(iface.getInetAddresses())) {
				if (isIpv6LinkLocalAddress(addr)) return addr;
			}
			// No suitable address
			return null;
		} catch (SocketException e) {
			logException(logger, WARNING, e);
			return null;
		}
	}

	private InetAddress intToInetAddress(int ip) {
		byte[] ipBytes = new byte[4];
		ipBytes[0] = (byte) (ip & 0xFF);
		ipBytes[1] = (byte) ((ip >> 8) & 0xFF);
		ipBytes[2] = (byte) ((ip >> 16) & 0xFF);
		ipBytes[3] = (byte) ((ip >> 24) & 0xFF);
		try {
			return InetAddress.getByAddress(ipBytes);
		} catch (UnknownHostException e) {
			// Should only be thrown if address has illegal length
			throw new AssertionError(e);
		}
	}

	private SocketFactory getSocketFactory() {
		for (Network net : connectivityManager.getAllNetworks()) {
			NetworkCapabilities caps =
					connectivityManager.getNetworkCapabilities(net);
			if (caps != null && caps.hasTransport(TRANSPORT_WIFI)) {
				return net.getSocketFactory();
			}
		}
		logger.warning("Could not find suitable socket factory");
		return SocketFactory.getDefault();
	}

	@Override
	public void onEventOccurred(Event e) {
		super.onEventOccurred(e);
		if (e instanceof NetworkStatusEvent) updateConnectionStatus();
	}

	private void updateConnectionStatus() {
		connectionStatusExecutor.execute(() -> {
			State s = getState();
			if (s != ACTIVE && s != INACTIVE) return;
			Pair<InetAddress, Boolean> wifi = getPreferredWifiAddress();
			if (wifi == null) {
				logger.info("Not connected to wifi");
				socketFactory = SocketFactory.getDefault();
				// Server sockets may not have been closed automatically when
				// interface was taken down. If any sockets are open, closing
				// them here will cause the sockets to be cleared and the state
				// to be updated in acceptContactConnections()
				if (s == ACTIVE) {
					logger.info("Closing server sockets");
					tryToClose(state.getServerSocket(true), logger, WARNING);
					tryToClose(state.getServerSocket(false), logger, WARNING);
				}
			} else if (wifi.getSecond()) {
				logger.info("Providing wifi hotspot");
				// There's no corresponding Network object and thus no way
				// to get a suitable socket factory, so we won't be able to
				// make outgoing connections on API 21+ if another network
				// has internet access
				socketFactory = SocketFactory.getDefault();
				bind();
			} else {
				logger.info("Connected to wifi");
				socketFactory = getSocketFactory();
				bind();
			}
		});
	}

	/**
	 * Returns a {@link Pair} where the first element is an IP address (IPv4 if
	 * available, otherwise IPv6) of the wifi interface and the second element
	 * is true if this device is providing an access point, or false if this
	 * device is a client. Returns null if this device isn't connected to wifi
	 * as an access point or client.
	 */
	private Pair<InetAddress, Boolean> getPreferredWifiAddress() {
		Pair<InetAddress, Boolean> wifi = getWifiIpv4Address();
		// If there's no wifi IPv4 address, we might be a client on an
		// IPv6-only wifi network. We can only detect this on API 21+
		if (wifi == null) {
			InetAddress ipv6 = getWifiClientIpv6Address();
			if (ipv6 != null) return new Pair<>(ipv6, false);
		}
		return wifi;
	}
}