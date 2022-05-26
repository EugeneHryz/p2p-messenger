package com.eugene.wc.protocol.connection;

import com.eugene.wc.protocol.api.connection.ConnectionManager;
import com.eugene.wc.protocol.api.connection.ConnectionRegistry;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionModule {

	@Singleton
	@Provides
	ConnectionManager provideConnectionManager(ConnectionManagerImpl connectionManager) {
		return connectionManager;
	}

	@Provides
	@Singleton
	ConnectionRegistry provideConnectionRegistry(ConnectionRegistryImpl connectionRegistry) {
		return connectionRegistry;
	}
}
