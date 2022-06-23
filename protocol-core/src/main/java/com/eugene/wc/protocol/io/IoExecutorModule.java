package com.eugene.wc.protocol.io;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IoExecutorModule {

    private final ExecutorService executorService;

    public IoExecutorModule() {
        BlockingQueue<Runnable> queue = new SynchronousQueue<>();
        RejectedExecutionHandler policy =
                new ThreadPoolExecutor.DiscardPolicy();
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
