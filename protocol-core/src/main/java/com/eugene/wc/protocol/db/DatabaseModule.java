package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.db.DatabaseConfig;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DatabaseModule {

    @Singleton
    @Provides
    public JdbcDatabase provideJdbcDatabase(DatabaseConfig dbConfig) {
        return new H2Database(dbConfig);
    }
}
