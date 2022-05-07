package com.eugene.wc.protocol.io;

import com.eugene.wc.protocol.api.io.IoExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;

@Module
public class IoExecutorModule {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 60;

    private final ExecutorService executorService;

    public IoExecutorModule() {

        RejectedExecutionHandler reh = new ThreadPoolExecutor.DiscardPolicy();
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
        executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, taskQueue, reh);
    }

    @Provides
    @IoExecutor
    public Executor provideExecutor() {
        return executorService;
    }

    @Provides
    public ExecutorService provideExecutorService() {
        return executorService;
    }
}
