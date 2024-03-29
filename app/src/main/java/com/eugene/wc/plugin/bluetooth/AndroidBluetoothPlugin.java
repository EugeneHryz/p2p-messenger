package com.eugene.wc.plugin.bluetooth;

import static android.bluetooth.BluetoothAdapter.*;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static com.eugene.wc.protocol.api.util.PrivacyUtils.scrubMacAddress;
import static java.util.Collections.shuffle;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.eugene.wc.protocol.api.plugin.Backoff;
import com.eugene.wc.protocol.api.plugin.PluginCallback;
import com.eugene.wc.protocol.api.plugin.PluginException;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.system.AndroidExecutor;
import com.eugene.wc.protocol.api.system.Clock;
import com.eugene.wc.protocol.api.util.IoUtils;
import com.eugene.wc.protocol.plugin.bluetooth.AbstractBluetoothPlugin;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionFactory;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionLimiter;
import com.eugene.wc.util.AndroidUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class AndroidBluetoothPlugin extends AbstractBluetoothPlugin<BluetoothSocket, BluetoothServerSocket> {

	private static final Logger logger = getLogger(AndroidBluetoothPlugin.class.getName());

	private static final int MAX_DISCOVERY_MS = 10_000;

	private final AndroidExecutor androidExecutor;
	private final Application app;
	private final Clock clock;

	private volatile BluetoothStateReceiver receiver = null;

	// Non-null if the plugin started successfully
	private volatile BluetoothAdapter adapter = null;
	private volatile boolean stopDiscoverAndConnect;

	public AndroidBluetoothPlugin(BluetoothConnectionLimiter connectionLimiter,
								  BluetoothConnectionFactory<BluetoothSocket> connectionFactory,
								  Executor ioExecutor,
								  Executor wakefulIoExecutor,
								  SecureRandom secureRandom,
								  AndroidExecutor androidExecutor,
								  Application app,
								  Clock clock,
								  Backoff backoff,
								  PluginCallback callback,
								  long maxLatency,
								  int maxIdleTime) {
		super(connectionLimiter, connectionFactory, ioExecutor,
				wakefulIoExecutor, secureRandom, backoff, callback,
				maxLatency, maxIdleTime);
		this.androidExecutor = androidExecutor;
		this.app = app;
		this.clock = clock;
	}

	@Override
	public void start() throws PluginException {
		super.start();
		// Listen for changes to the Bluetooth state
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_STATE_CHANGED);
		filter.addAction(ACTION_SCAN_MODE_CHANGED);
		receiver = new BluetoothStateReceiver();
		app.registerReceiver(receiver, filter);
	}

	@Override
	public void stop() {
		super.stop();
		if (receiver != null) {
			app.unregisterReceiver(receiver);
		}
	}

	@Override
	public void initialiseAdapter() throws IOException {
		// BluetoothAdapter.getDefaultAdapter() must be called on a thread
		// with a message queue, so submit it to the AndroidExecutor
		try {
			adapter = androidExecutor.runOnBackgroundThread(BluetoothAdapter::getDefaultAdapter)
					.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException(e);
		}
		if (adapter == null) {
			throw new IOException("Bluetooth is not supported");
		}
	}

	@Override
	public boolean isAdapterEnabled() {
		return adapter != null && adapter.isEnabled();
	}

	@Override
	public String getBluetoothAddress() {
		if (adapter == null) return null;
		String address = AndroidUtils.getBluetoothAddress(app, adapter);
		return address.isEmpty() ? null : address;
	}

	@Override
	public BluetoothServerSocket openServerSocket(String uuid) throws IOException {
		return adapter.listenUsingInsecureRfcommWithServiceRecord(
				"RFCOMM", UUID.fromString(uuid));
	}

	@Override
	public void tryToClose(BluetoothServerSocket ss) {
		IoUtils.tryToClose(ss, logger, WARNING);
	}

	@Override
	public DuplexTransportConnection acceptConnection(BluetoothServerSocket ss)
			throws IOException {
		return connectionFactory.wrapSocket(this, ss.accept());
	}

	@Override
	public boolean isValidAddress(String address) {
		return checkBluetoothAddress(address);
	}

	@Override
	public DuplexTransportConnection connectTo(String address, String uuid)
			throws IOException {
		BluetoothDevice d = adapter.getRemoteDevice(address);
		UUID u = UUID.fromString(uuid);
		BluetoothSocket s = null;
		try {
			s = d.createInsecureRfcommSocketToServiceRecord(u);
			s.connect();
			return connectionFactory.wrapSocket(this, s);
		} catch (IOException e) {
			IoUtils.tryToClose(s, logger, WARNING);
			throw e;
		} catch (NullPointerException e) {
			// BluetoothSocket#connect() may throw an NPE under unknown
			// circumstances
			IoUtils.tryToClose(s, logger, WARNING);
			throw new IOException(e);
		}
	}

	@Override
	public DuplexTransportConnection discoverAndConnect(String uuid) {
		if (adapter == null) return null;
		if (!discoverSemaphore.tryAcquire()) {
			logger.info("Discover already running");
			return null;
		}
		try {
			stopDiscoverAndConnect = false;
			for (String address : discoverDevices()) {
				if (stopDiscoverAndConnect) {
					break;
				}
				try {
					if (logger.isLoggable(INFO))
						logger.info("Connecting to " + scrubMacAddress(address));
					return connectTo(address, uuid);
				} catch (IOException e) {
					if (logger.isLoggable(INFO)) {
						logger.info("Could not connect to " + scrubMacAddress(address));
					}
				}
			}
		} finally {
			discoverSemaphore.release();
		}
		logger.info("Could not connect to any devices");
		return null;
	}

	@Override
	public void stopDiscoverAndConnect() {
		stopDiscoverAndConnect = true;
		adapter.cancelDiscovery();
	}

	private Collection<String> discoverDevices() {
		List<String> addresses = new ArrayList<>();
		BlockingQueue<Intent> intents = new LinkedBlockingQueue<>();
		DiscoveryReceiver receiver = new DiscoveryReceiver(intents);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_DISCOVERY_STARTED);
		filter.addAction(ACTION_DISCOVERY_FINISHED);
		filter.addAction(ACTION_FOUND);
		app.registerReceiver(receiver, filter);
		try {
			if (adapter.startDiscovery()) {
				long now = clock.currentTimeMillis();
				long end = now + MAX_DISCOVERY_MS;
				while (now < end) {
					Intent i = intents.poll(end - now, MILLISECONDS);
					if (i == null) break;
					String action = i.getAction();
					if (ACTION_DISCOVERY_STARTED.equals(action)) {
						logger.info("Discovery started");
					} else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
						logger.info("Discovery finished");
						break;
					} else if (ACTION_FOUND.equals(action)) {
						BluetoothDevice d = i.getParcelableExtra(EXTRA_DEVICE);
						// Ignore Bluetooth LE devices
						if (d.getType() != DEVICE_TYPE_LE) {
							String address = d.getAddress();
							if (logger.isLoggable(INFO))
								logger.info("Discovered " +
										scrubMacAddress(address));
							if (!addresses.contains(address))
								addresses.add(address);
						}
					}
					now = clock.currentTimeMillis();
				}
			} else {
				logger.info("Could not start discovery");
			}
		} catch (InterruptedException e) {
			logger.info("Interrupted while discovering devices");
			Thread.currentThread().interrupt();
		} finally {
			logger.info("Cancelling discovery");
			adapter.cancelDiscovery();
			app.unregisterReceiver(receiver);
		}
		// Shuffle the addresses so we don't always try the same one first
		shuffle(addresses);
		return addresses;
	}

	private class BluetoothStateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			int state = intent.getIntExtra(EXTRA_STATE, 0);
			if (state == STATE_ON) onAdapterEnabled();
			else if (state == STATE_OFF) onAdapterDisabled();
			int scanMode = intent.getIntExtra(EXTRA_SCAN_MODE, 0);
			if (scanMode == SCAN_MODE_NONE) {
				logger.info("Scan mode: None");
			} else if (scanMode == SCAN_MODE_CONNECTABLE) {
				logger.info("Scan mode: Connectable");
			} else if (scanMode == SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				logger.info("Scan mode: Discoverable");
			}
		}
	}

	private static class DiscoveryReceiver extends BroadcastReceiver {

		private final BlockingQueue<Intent> intents;

		private DiscoveryReceiver(BlockingQueue<Intent> intents) {
			this.intents = intents;
		}

		@Override
		public void onReceive(Context ctx, Intent intent) {
			intents.add(intent);
		}
	}
}
