package com.eugene.wc;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.eugene.wc.work.WorkerFactory;

import javax.inject.Inject;

public class MessengerApplicationImpl extends Application implements MessengerApplication {

    private ApplicationComponent appComponent;

    @Inject
    WorkerFactory customWorkerFactory;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerApplicationComponent.builder()
                .appModule(new AppModule(this))
                .build();
        AndroidEagerSingletons.Helper.injectEagerSingletons(appComponent);


        appComponent.inject(this);
        Configuration workConfig = new Configuration.Builder()
                .setWorkerFactory(customWorkerFactory)
                .build();
        WorkManager.initialize(this, workConfig);
    }

    @Override
    public ApplicationComponent getApplicationComponent() {
        return appComponent;
    }
}
