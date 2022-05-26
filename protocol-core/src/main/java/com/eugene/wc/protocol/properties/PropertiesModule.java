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
//		@Inject
//		TransportPropertyValidator transportPropertyValidator;
		@Inject
		TransportPropertyManager transportPropertyManager;
	}

//	@Provides
//	@Singleton
//	TransportPropertyValidator getValidator(ValidationManager validationManager,
//			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
//			Clock clock) {
//		TransportPropertyValidator validator = new TransportPropertyValidator(
//				clientHelper, metadataEncoder, clock);
//		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
//				validator);
//		return validator;
//	}

	@Provides
	@Singleton
	TransportPropertyManager getTransportPropertyManager(LifecycleManager lifecycleManager,
			ContactManager contactManager, TransportPropertyManagerImpl transportPropertyManager) {

		System.out.println("PROVIDING INSTANCE OF TRANSPORT PROPERTY MANAGER");
		lifecycleManager.registerDatabaseOpenListener(transportPropertyManager);
//		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
//				transportPropertyManager);
		contactManager.registerContactHook(transportPropertyManager);
//		clientVersioningManager.registerClient(CLIENT_ID, MAJOR_VERSION,
//				MINOR_VERSION, transportPropertyManager);
		return transportPropertyManager;
	}
}
