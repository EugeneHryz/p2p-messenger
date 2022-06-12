package com.eugene.wc.protocol.conversation;

import com.eugene.wc.protocol.api.conversation.ConversationManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ConversationModule {

    @Provides
    @Singleton
    public ConversationManager provideConversationManager(ConversationManagerImpl cm) {
        return cm;
    }
}
