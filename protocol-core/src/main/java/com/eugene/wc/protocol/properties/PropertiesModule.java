package com.eugene.wc.protocol.properties;

import com.eugene.wc.protocol.api.contact.ContactManager;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;
import com.eugene.wc.protocol.api.properties.TransportPropertyManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PropertiesModule {

	public static class EagerSingletons {
		@Inject
		TransportPropertyManager transportPropertyManager;
	}

	@Provides
	@Singleton
	TransportPropertyManager getTransportPropertyManager(LifecycleManager lifecycleManager,
														 ContactManager contactManager,
														 TransportPropertyManagerImpl tpm) {
		lifecycleManager.registerDatabaseOpenListener(tpm);
		contactManager.registerContactHook(tpm);
		return tpm;
	}
}
