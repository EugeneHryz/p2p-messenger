package com.eugene.wc.plugin.bluetooth;

import static com.eugene.wc.protocol.api.plugin.BluetoothConstants.ID;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.plugin.Backoff;
import com.eugene.wc.protocol.api.plugin.PluginCallback;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPluginFactory;
import com.eugene.wc.protocol.api.system.AndroidExecutor;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.api.system.Clock;
import com.eugene.wc.protocol.api.system.WakefulIoExecutor;
import com.eugene.wc.protocol.plugin.BackoffImpl;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionFactory;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionLimiter;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionLimiterImpl;

import java.security.SecureRandom;
import java.util.concurrent.Executor;

import javax.inject.Inject;

public class AndroidBluetoothPluginFactory implements DuplexPluginFactory {

	private static final int MAX_LATENCY = 30 * 1000; // 30 seconds
	private static final int MAX_IDLE_TIME = 30 * 1000; // 30 seconds
	private static final int MIN_POLLING_INTERVAL = 60 * 1000; // 1 minute
	private static final int MAX_POLLING_INTERVAL = 10 * 60 * 1000; // 10 mins
	private static final double BACKOFF_BASE = 1.2;

	private final Executor ioExecutor, wakefulIoExecutor;
	private final AndroidExecutor androidExecutor;
	private final AndroidWakeLockManager wakeLockManager;
	private final Application app;
	private final SecureRandom secureRandom;
	private final EventBus eventBus;
	private final Clock clock;

	@Inject
    public AndroidBluetoothPluginFactory(@IoExecutor Executor ioExecutor,
                                  @WakefulIoExecutor Executor wakefulIoExecutor,
                                  AndroidExecutor androidExecutor,
                                  AndroidWakeLockManager wakeLockManager,
                                  Application app,
                                  EventBus eventBus,
                                  Clock clock) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.androidExecutor = androidExecutor;
		this.wakeLockManager = wakeLockManager;
		this.app = app;
		this.secureRandom = new SecureRandom();
		this.eventBus = eventBus;
		this.clock = clock;
	}

	@Override
	public TransportId getId() {
		return ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Override
	public DuplexPlugin createPlugin(PluginCallback callback) {
		BluetoothConnectionLimiter connectionLimiter = new BluetoothConnectionLimiterImpl(eventBus);
		BluetoothConnectionFactory<BluetoothSocket> connectionFactory =
				new AndroidBluetoothConnectionFactory(connectionLimiter, wakeLockManager);
		Backoff backoff = new BackoffImpl(MIN_POLLING_INTERVAL, MAX_POLLING_INTERVAL, BACKOFF_BASE);
		AndroidBluetoothPlugin plugin = new AndroidBluetoothPlugin(
				connectionLimiter, connectionFactory, ioExecutor,
				wakefulIoExecutor, secureRandom, androidExecutor, app,
				clock, backoff, callback, MAX_LATENCY, MAX_IDLE_TIME);
		eventBus.addListener(plugin);
		return plugin;
	}
}
