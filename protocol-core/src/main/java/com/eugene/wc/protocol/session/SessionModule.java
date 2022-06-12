package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.api.session.GroupFactory;
import com.eugene.wc.protocol.api.session.MessageFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class SessionModule {

	@Provides
	GroupFactory provideGroupFactory(GroupFactoryImpl groupFactory) {
		return groupFactory;
	}

	@Provides
	MessageFactory provideMessageFactory(MessageFactoryImpl messageFactory) {
		return messageFactory;
	}
}
