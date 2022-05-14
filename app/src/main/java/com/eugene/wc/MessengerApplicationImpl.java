package com.eugene.wc;

import android.app.Application;

public class MessengerApplicationImpl extends Application implements MessengerApplication {

    private ApplicationComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerApplicationComponent.builder()
                .appModule(new AppModule(this))
                .build();

        AndroidEagerSingletons.Helper.injectEagerSingletons(appComponent);
    }

    @Override
    public ApplicationComponent getApplicationComponent() {
        return appComponent;
    }
}
