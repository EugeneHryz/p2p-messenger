package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.DatabaseConfig;
import com.eugene.wc.protocol.api.db.DbExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DatabaseModule {

    private final ExecutorService dbExecutor;

    public DatabaseModule() {
        RejectedExecutionHandler reh = new ThreadPoolExecutor.DiscardPolicy();
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
        dbExecutor = new ThreadPoolExecutor(0, 1, 60,
                TimeUnit.SECONDS, taskQueue, reh);
    }

    @Singleton
    @Provides
    public JdbcDatabase provideJdbcDatabase(DatabaseConfig dbConfig) {
        return new H2Database(dbConfig);
    }

    @Singleton
    @Provides
    public DatabaseComponent provideDatabaseComponent(DatabaseComponentImpl dbComponent) {
        return dbComponent;
    }

    @Singleton
    @Provides
    @DbExecutor
    public Executor provideDbExecutor() {
        return dbExecutor;
    }
}
