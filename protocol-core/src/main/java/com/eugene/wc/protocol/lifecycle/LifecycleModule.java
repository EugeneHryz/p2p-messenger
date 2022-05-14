package com.eugene.wc.protocol.lifecycle;

import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LifecycleModule {

    @Singleton
    @Provides
    LifecycleManager provideLifecycleManager(LifecycleManagerImpl lifecycleManager) {
        return lifecycleManager;
    }
}
