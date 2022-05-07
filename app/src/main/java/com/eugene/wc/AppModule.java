package com.eugene.wc;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.eugene.wc.protocol.api.db.DatabaseConfig;

import java.io.File;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private final static String DB_DIR = "db";
    private final static String DB_KEY_DIR = "db.key";

    private final Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides
    DatabaseConfig provideDatabaseConfig() {
        StrictMode.ThreadPolicy tp = StrictMode.allowThreadDiskWrites();
        StrictMode.allowThreadDiskReads();

        File dbDir = application.getApplicationContext().getDir(DB_DIR, Context.MODE_PRIVATE);
        File dbKeyDir = application.getApplicationContext().getDir(DB_KEY_DIR, Context.MODE_PRIVATE);

        StrictMode.setThreadPolicy(tp);
        return new DatabaseConfig(dbKeyDir, dbDir);
    }
}
