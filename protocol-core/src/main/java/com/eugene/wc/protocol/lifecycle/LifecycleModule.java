package com.eugene.wc.protocol.lifecycle;

import com.eugene.wc.protocol.api.lifecycle.EventExecutor;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LifecycleModule {

    private final ExecutorService executorService;

    public LifecycleModule() {
        executorService = Executors.newFixedThreadPool(1);
    }

    @EventExecutor
    @Provides
    Executor provideEventExecutor() {
        return executorService;
    }

    @Singleton
    @Provides
    LifecycleManager provideLifecycleManager(LifecycleManagerImpl lifecycleManager) {
        return lifecycleManager;
    }
}
