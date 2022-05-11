package com.eugene.wc.protocol.event;

import com.eugene.wc.protocol.api.event.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class EventModule {

    @Singleton
    @Provides
    public EventBus provideEventBus(EventBusImpl eventBus) {
        return eventBus;
    }
}
