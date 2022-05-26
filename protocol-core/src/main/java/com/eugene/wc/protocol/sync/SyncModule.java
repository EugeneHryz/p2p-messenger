package com.eugene.wc.protocol.sync;

import com.eugene.wc.protocol.api.sync.GroupFactory;
import com.eugene.wc.protocol.api.sync.MessageFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class SyncModule {

	@Provides
	GroupFactory provideGroupFactory(GroupFactoryImpl groupFactory) {
		return groupFactory;
	}

	@Provides
	MessageFactory provideMessageFactory(MessageFactoryImpl messageFactory) {
		return messageFactory;
	}
}
