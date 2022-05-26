package com.eugene.wc.protocol.client;

import com.eugene.wc.protocol.api.client.ClientHelper;
import com.eugene.wc.protocol.api.client.ContactGroupFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class ClientModule {

	@Provides
	ClientHelper provideClientHelper(ClientHelperImpl clientHelper) {
		return clientHelper;
	}

	@Provides
	ContactGroupFactory provideContactGroupFactory(ContactGroupFactoryImpl contactGroupFactory) {
		return contactGroupFactory;
	}
}
