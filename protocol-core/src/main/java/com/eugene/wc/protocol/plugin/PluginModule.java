package com.eugene.wc.protocol.plugin;

import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;
import com.eugene.wc.protocol.api.plugin.PluginConfig;
import com.eugene.wc.protocol.api.plugin.PluginManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PluginModule {

	public static class EagerSingletons {
		@Inject
		PluginManager pluginManager;
		@Inject
		Poller poller;
	}

	@Provides
	@Singleton
	public PluginManager providePluginManager(LifecycleManager lifecycleManager,
									   PluginManagerImpl pluginManager) {
		lifecycleManager.registerService(pluginManager);
		return pluginManager;
	}

	@Provides
	@Singleton
	public Poller providePoller(PluginConfig config,
						 EventBus eventBus,
						 PollerImpl poller) {
		if (config.shouldPoll()) eventBus.addListener(poller);
		return poller;
	}
}
