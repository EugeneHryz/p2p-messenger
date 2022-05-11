package com.eugene.wc.protocol.plugin.tcp;

import static com.eugene.wc.protocol.api.plugin.LanTcpConstants.ID;

import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.plugin.Backoff;
import com.eugene.wc.protocol.api.plugin.BackoffFactory;
import com.eugene.wc.protocol.api.plugin.PluginCallback;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPluginFactory;
import com.eugene.wc.protocol.api.system.WakefulIoExecutor;

import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
public class LanTcpPluginFactory implements DuplexPluginFactory {

	private static final int MAX_LATENCY = 30_000; // 30 seconds
	private static final int MAX_IDLE_TIME = 30_000; // 30 seconds
	private static final int CONNECTION_TIMEOUT = 3_000; // 3 seconds
	private static final int MIN_POLLING_INTERVAL = 60_000; // 1 minute
	private static final int MAX_POLLING_INTERVAL = 600_000; // 10 mins
	private static final double BACKOFF_BASE = 1.2;

	private final Executor ioExecutor, wakefulIoExecutor;
	private final EventBus eventBus;
	private final BackoffFactory backoffFactory;

	@Inject
	public LanTcpPluginFactory(@IoExecutor Executor ioExecutor,
			@WakefulIoExecutor Executor wakefulIoExecutor,
			EventBus eventBus,
			BackoffFactory backoffFactory) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.eventBus = eventBus;
		this.backoffFactory = backoffFactory;
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
		Backoff backoff = backoffFactory.createBackoff(MIN_POLLING_INTERVAL,
				MAX_POLLING_INTERVAL, BACKOFF_BASE);
		LanTcpPlugin plugin = new LanTcpPlugin(ioExecutor, wakefulIoExecutor,
				backoff, callback, MAX_LATENCY, MAX_IDLE_TIME,
				CONNECTION_TIMEOUT);
		eventBus.addListener(plugin);
		return plugin;
	}
}
