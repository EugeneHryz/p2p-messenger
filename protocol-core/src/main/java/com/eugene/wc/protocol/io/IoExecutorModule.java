package com.eugene.wc.protocol.io;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IoExecutorModule {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 8;
    private static final int KEEP_ALIVE_TIME = 60;

    private final ExecutorService executorService;

    public IoExecutorModule() {

        // The thread pool is unbounded, so use direct handoff
        BlockingQueue<Runnable> queue = new SynchronousQueue<>();
        // Discard tasks that are submitted during shutdown
        RejectedExecutionHandler policy =
                new ThreadPoolExecutor.DiscardPolicy();
        // Create threads as required and keep them in the pool for 60 seconds
        executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60, SECONDS, queue, policy);
    }

    @Provides
    @Singleton
    @IoExecutor
    public Executor provideExecutor(LifecycleManager lifecycleManager) {
        lifecycleManager.registerForShutdown(executorService);
        return executorService;
    }
}
