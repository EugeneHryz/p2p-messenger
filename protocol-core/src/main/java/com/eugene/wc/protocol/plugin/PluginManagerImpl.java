package com.eugene.wc.protocol.plugin;

import static com.eugene.wc.protocol.api.plugin.Plugin.PREF_PLUGIN_ENABLE;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.ACTIVE;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.DISABLED;
import static com.eugene.wc.protocol.api.plugin.Plugin.State.STARTING_STOPPING;
import static com.eugene.wc.protocol.api.util.LogUtils.logDuration;
import static com.eugene.wc.protocol.api.util.LogUtils.logException;
import static com.eugene.wc.protocol.api.util.LogUtils.now;
import static java.util.Collections.emptyList;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.lifecycle.Service;
import com.eugene.wc.protocol.api.lifecycle.exception.ServiceException;
import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.PluginCallback;
import com.eugene.wc.protocol.api.plugin.PluginConfig;
import com.eugene.wc.protocol.api.plugin.PluginException;
import com.eugene.wc.protocol.api.plugin.PluginManager;
import com.eugene.wc.protocol.api.plugin.TransportConnectionReader;
import com.eugene.wc.protocol.api.plugin.TransportConnectionWriter;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPluginFactory;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.plugin.event.TransportActiveEvent;
import com.eugene.wc.protocol.api.plugin.event.TransportInactiveEvent;
import com.eugene.wc.protocol.api.plugin.event.TransportStateEvent;
import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.api.settings.SettingsManager;
import com.eugene.wc.protocol.api.system.WakefulIoExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

@ThreadSafe
class PluginManagerImpl implements PluginManager, Service {

	private static final Logger LOG =
			getLogger(PluginManagerImpl.class.getName());

	private final Executor ioExecutor, wakefulIoExecutor;
	private final EventBus eventBus;
	private final PluginConfig pluginConfig;
	private final SettingsManager settingsManager;
//	private final TransportPropertyManager transportPropertyManager;
	private final Map<TransportId, Plugin> plugins;
	private final List<DuplexPlugin> duplexPlugins;
	private final Map<TransportId, CountDownLatch> startLatches;
	private final AtomicBoolean used = new AtomicBoolean(false);

	@Inject
	PluginManagerImpl(@IoExecutor Executor ioExecutor,
			@WakefulIoExecutor Executor wakefulIoExecutor,
			EventBus eventBus,
			PluginConfig pluginConfig,
			SettingsManager settingsManager) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.eventBus = eventBus;
		this.pluginConfig = pluginConfig;
		this.settingsManager = settingsManager;
//		this.transportPropertyManager = transportPropertyManager;
		plugins = new ConcurrentHashMap<>();
		duplexPlugins = new CopyOnWriteArrayList<>();
		startLatches = new ConcurrentHashMap<>();
	}

	@Override
	public void startService() {
		if (used.getAndSet(true)) throw new IllegalStateException();

		// Instantiate the duplex plugins and start them asynchronously
		LOG.info("Starting duplex plugins");
		for (DuplexPluginFactory f : pluginConfig.getDuplexFactories()) {
			TransportId t = f.getId();
			LOG.info("About to create plugin for: " + f.getId().getString());
			DuplexPlugin d = f.createPlugin(new Callback(t));
			if (d == null) {
				if (LOG.isLoggable(WARNING))
					LOG.warning("Could not create plugin for " + t);
			} else {
				LOG.info("Launching PluginStarter...");
				plugins.put(t, d);
				duplexPlugins.add(d);
				CountDownLatch startLatch = new CountDownLatch(1);
				startLatches.put(t, startLatch);
				wakefulIoExecutor.execute(new PluginStarter(d, startLatch));
			}
		}
	}

	@Override
	public void stopService() throws ServiceException {
		CountDownLatch stopLatch = new CountDownLatch(plugins.size());

		// Stop the duplex plugins
		LOG.info("Stopping duplex plugins");
		for (DuplexPlugin d : duplexPlugins) {
			CountDownLatch startLatch = startLatches.get(d.getId());
			// Don't need the wakeful executor here as we wait for the plugin
			// to stop before returning
			ioExecutor.execute(new PluginStopper(d, startLatch, stopLatch));
		}
		// Wait for all the plugins to stop
		try {
			LOG.info("Waiting for all the plugins to stop");
			stopLatch.await();
		} catch (InterruptedException e) {
			throw new ServiceException(e);
		}
	}

	@Override
	public Plugin getPlugin(TransportId t) {
		return plugins.get(t);
	}

	@Override
	public Collection<DuplexPlugin> getDuplexPlugins() {
		return new ArrayList<>(duplexPlugins);
	}

	@Override
	public Collection<DuplexPlugin> getKeyAgreementPlugins() {
		List<DuplexPlugin> supported = new ArrayList<>();
		for (DuplexPlugin d : duplexPlugins)
			if (d.supportsKeyAgreement()) supported.add(d);
		return supported;
	}

	@Override
	public Collection<DuplexPlugin> getRendezvousPlugins() {
		List<DuplexPlugin> supported = new ArrayList<>();
		for (DuplexPlugin d : duplexPlugins)
			if (d.supportsRendezvous()) supported.add(d);
		return supported;
	}

	@Override
	public void setPluginEnabled(TransportId t, boolean enabled) {
		Plugin plugin = plugins.get(t);
		if (plugin == null) return;

		Settings s = new Settings();
		s.putBoolean(PREF_PLUGIN_ENABLE, enabled);
		ioExecutor.execute(() -> mergeSettings(s, t.getString()));
	}

	private void mergeSettings(Settings s, String namespace) {
		try {
			long start = now();
			settingsManager.mergeSettings(s, namespace);
			logDuration(LOG, "Merging settings", start);
		} catch (DbException e) {
			logException(LOG, WARNING, e);
		}
	}

	private static class PluginStarter implements Runnable {

		private final Plugin plugin;
		private final CountDownLatch startLatch;

		private PluginStarter(Plugin plugin, CountDownLatch startLatch) {
			LOG.info("In pluginStarter constructor");
			this.plugin = plugin;
			this.startLatch = startLatch;
		}

		@Override
		public void run() {
			try {
				long start = now();
				LOG.info("BEFORE plugin.start()");
				plugin.start();
				LOG.info("AFTER plugin.start()");
				logDuration(LOG, "Starting plugin " + plugin.getId(),
						start);

			} catch (PluginException e) {
				if (LOG.isLoggable(WARNING)) {
					LOG.warning("Plugin " + plugin.getId() + " did not start");
					logException(LOG, WARNING, e);
				}
			} finally {
				startLatch.countDown();
			}
		}
	}

	private static class PluginStopper implements Runnable {

		private final Plugin plugin;
		private final CountDownLatch startLatch, stopLatch;

		private PluginStopper(Plugin plugin, CountDownLatch startLatch,
				CountDownLatch stopLatch) {
			this.plugin = plugin;
			this.startLatch = startLatch;
			this.stopLatch = stopLatch;
		}

		@Override
		public void run() {
			if (LOG.isLoggable(INFO))
				LOG.info("Trying to stop plugin " + plugin.getId());
			try {
				// Wait for the plugin to finish starting
				startLatch.await();
				// Stop the plugin
				long start = now();
				plugin.stop();
				if (LOG.isLoggable(INFO)) {
					logDuration(LOG, "Stopping plugin " + plugin.getId(),
							start);
				}
			} catch (InterruptedException e) {
				LOG.warning("Interrupted while waiting for plugin to stop");
				// This task runs on an executor, so don't reset the interrupt
			} catch (PluginException e) {
				if (LOG.isLoggable(WARNING)) {
					LOG.warning("Plugin " + plugin.getId() + " did not stop");
					logException(LOG, WARNING, e);
				}
			} finally {
				stopLatch.countDown();
			}
		}
	}

	private class Callback implements PluginCallback {

		private final TransportId id;
		private final AtomicReference<Plugin.State> state =
				new AtomicReference<>(STARTING_STOPPING);

		private Callback(TransportId id) {
			this.id = id;
		}

		@Override
		public Settings getSettings() {
			try {
				return settingsManager.getSettings(id.getString());
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				return new Settings();
			}
		}

		@Override
		public TransportProperties getLocalProperties() {
//			try {
//				return transportPropertyManager.getLocalProperties(id);
//			} catch (DbException e) {
//				logException(LOG, WARNING, e);
//				return new TransportProperties();
//			}
			return new TransportProperties();
		}

		@Override
		public Collection<TransportProperties> getRemoteProperties() {
//			try {
//				Map<ContactId, TransportProperties> remote =
//						transportPropertyManager.getRemoteProperties(id);
//				return remote.values();
//			} catch (DbException e) {
//				logException(LOG, WARNING, e);
//				return emptyList();
//			}
			return emptyList();
		}

		@Override
		public void mergeSettings(Settings s) {
			PluginManagerImpl.this.mergeSettings(s, id.getString());
		}

		@Override
		public void mergeLocalProperties(TransportProperties p) {
//			try {
//				transportPropertyManager.mergeLocalProperties(id, p);
//			} catch (DbException e) {
//				logException(LOG, WARNING, e);
//			}
		}

		@Override
		public void pluginStateChanged(Plugin.State newState) {
			Plugin.State oldState = state.getAndSet(newState);
			if (newState != oldState) {
				if (LOG.isLoggable(INFO)) {
					LOG.info(id + " changed from state " + oldState
							+ " to " + newState);
				}
				eventBus.broadcast(new TransportStateEvent(id, newState));
				if (newState == ACTIVE) {
					eventBus.broadcast(new TransportActiveEvent(id));
				} else if (oldState == ACTIVE) {
					eventBus.broadcast(new TransportInactiveEvent(id));
				}
			} else if (newState == DISABLED) {
				// Broadcast an event even though the state hasn't changed, as
				// the reasons for the plugin being disabled may have changed
				eventBus.broadcast(new TransportStateEvent(id, newState));
			}
		}

		@Override
		public void handleConnection(DuplexTransportConnection d) {
//			connectionManager.manageIncomingConnection(id, d);
		}

		@Override
		public void handleReader(TransportConnectionReader r) {
//			connectionManager.manageIncomingConnection(id, r);
		}

		@Override
		public void handleWriter(TransportConnectionWriter w) {
			// TODO: Support simplex plugins that write to incoming connections
			throw new UnsupportedOperationException();
		}
	}
}
