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

import com.eugene.wc.protocol.api.connection.ConnectionManager;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;
import com.eugene.wc.protocol.api.contact.ContactId;
import com.eugene.wc.protocol.api.data.WdfReader;
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
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;
import com.eugene.wc.protocol.api.settings.Settings;
import com.eugene.wc.protocol.api.settings.SettingsManager;
import com.eugene.wc.protocol.api.system.WakefulIoExecutor;
import com.eugene.wc.protocol.data.WdfReaderImpl;

import java.io.IOException;
import java.io.InputStream;
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
public class PluginManagerImpl implements PluginManager, Service {

	private static final Logger logger = getLogger(PluginManagerImpl.class.getName());

	private final Executor ioExecutor, wakefulIoExecutor;
	private final EventBus eventBus;
	private final PluginConfig pluginConfig;
	private final SettingsManager settingsManager;
	private final TransportPropertyManager tpm;
	private final Map<TransportId, Plugin> plugins;
	private final List<DuplexPlugin> duplexPlugins;
	private final Map<TransportId, CountDownLatch> startLatches;
	private final AtomicBoolean used = new AtomicBoolean(false);

	private final ConnectionManager connectionManager;

	@Inject
	public PluginManagerImpl(@IoExecutor Executor ioExecutor,
							 @WakefulIoExecutor Executor wakefulIoExecutor,
							 EventBus eventBus,
							 PluginConfig pluginConfig,
							 SettingsManager settingsManager,
							 TransportPropertyManager tpm,
							 ConnectionManager connectionManager) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.eventBus = eventBus;
		this.pluginConfig = pluginConfig;
		this.settingsManager = settingsManager;
		this.tpm = tpm;
		this.connectionManager = connectionManager;

		plugins = new ConcurrentHashMap<>();
		duplexPlugins = new CopyOnWriteArrayList<>();
		startLatches = new ConcurrentHashMap<>();
	}

	@Override
	public void startService() {
		if (used.getAndSet(true)) throw new IllegalStateException();

		// Instantiate the duplex plugins and start them asynchronously
		logger.info("Starting duplex plugins");
		for (DuplexPluginFactory f : pluginConfig.getDuplexFactories()) {
			TransportId t = f.getId();
			logger.info("About to create plugin for: " + f.getId().toString());
			DuplexPlugin d = f.createPlugin(new Callback(t));
			if (d == null) {
				if (logger.isLoggable(WARNING))
					logger.warning("Could not create plugin for " + t);
			} else {
				logger.info("Launching PluginStarter...");
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
		logger.info("Stopping duplex plugins");
		for (DuplexPlugin d : duplexPlugins) {
			CountDownLatch startLatch = startLatches.get(d.getId());
			// Don't need the wakeful executor here as we wait for the plugin
			// to stop before returning
			ioExecutor.execute(new PluginStopper(d, startLatch, stopLatch));
		}
		// Wait for all the plugins to stop
		try {
			logger.info("Waiting for all the plugins to stop");
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
	public void setPluginEnabled(TransportId t, boolean enabled) {
		Plugin plugin = plugins.get(t);
		if (plugin == null) return;

		Settings s = new Settings();
		s.putBoolean(PREF_PLUGIN_ENABLE, enabled);
		ioExecutor.execute(() -> mergeSettings(s, t.toString()));
	}

	private void mergeSettings(Settings s, String namespace) {
		try {
			long start = now();
			settingsManager.mergeSettings(s, namespace);
			logDuration(logger, "Merging settings", start);
		} catch (DbException e) {
			logException(logger, WARNING, e);
		}
	}

	private static class PluginStarter implements Runnable {

		private final Plugin plugin;
		private final CountDownLatch startLatch;

		private PluginStarter(Plugin plugin, CountDownLatch startLatch) {
			logger.info("In pluginStarter constructor");
			this.plugin = plugin;
			this.startLatch = startLatch;
		}

		@Override
		public void run() {
			try {
				long start = now();
				plugin.start();
				logDuration(logger, "Starting plugin " + plugin.getId(),
						start);

			} catch (PluginException e) {
				if (logger.isLoggable(WARNING)) {
					logger.warning("Plugin " + plugin.getId() + " did not start");
					logException(logger, WARNING, e);
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
			if (logger.isLoggable(INFO))
				logger.info("Trying to stop plugin " + plugin.getId());
			try {
				// Wait for the plugin to finish starting
				startLatch.await();
				// Stop the plugin
				long start = now();
				plugin.stop();
				if (logger.isLoggable(INFO)) {
					logDuration(logger, "Stopping plugin " + plugin.getId(),
							start);
				}
			} catch (InterruptedException e) {
				logger.warning("Interrupted while waiting for plugin to stop");
				// This task runs on an executor, so don't reset the interrupt
			} catch (PluginException e) {
				if (logger.isLoggable(WARNING)) {
					logger.warning("Plugin " + plugin.getId() + " did not stop");
					logException(logger, WARNING, e);
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
				return settingsManager.getSettings(id.toString());
			} catch (DbException e) {
				logException(logger, WARNING, e);
				return new Settings();
			}
		}

		@Override
		public TransportProperties getLocalProperties() {
			try {
				return tpm.getLocalProperties(id);
			} catch (DbException e) {
				logger.warning("Unable to get local properties\n" + e);
			}
			return new TransportProperties();
		}

		@Override
		public Collection<TransportProperties> getRemoteProperties() {
			try {
				Map<ContactId, TransportProperties> remote = tpm.getRemoteProperties(id);
				return remote.values();
			} catch (DbException e) {
				logger.warning("Unable to get remote properties\n" + e);
				return emptyList();
			}
		}

		@Override
		public void mergeSettings(Settings s) {
			PluginManagerImpl.this.mergeSettings(s, id.toString());
		}

		@Override
		public void mergeLocalProperties(TransportProperties p) {
			try {
				tpm.mergeLocalProperties(id, p);
			} catch (DbException e) {
				logger.warning("Unable to merge local properties\n" + e);
			}
		}

		@Override
		public void pluginStateChanged(Plugin.State newState) {
			Plugin.State oldState = state.getAndSet(newState);
			if (newState != oldState) {
				if (logger.isLoggable(INFO)) {
					logger.info(id + " changed from state " + oldState
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
			connectionManager.manageIncomingConnection(d, this.id);
		}
	}
}
