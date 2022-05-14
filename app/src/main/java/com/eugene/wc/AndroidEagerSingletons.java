package com.eugene.wc;

import com.eugene.wc.network.AndroidNetworkModule;
import com.eugene.wc.system.AndroidTaskSchedulerModule;

public interface AndroidEagerSingletons {

    void inject(AndroidNetworkModule.EagerSingletons singletons);

    void inject(AndroidTaskSchedulerModule.EagerSingletons singletons);

    class Helper {

        public static void injectEagerSingletons(AndroidEagerSingletons a) {
            a.inject(new AndroidNetworkModule.EagerSingletons());
            a.inject(new AndroidTaskSchedulerModule.EagerSingletons());
        }
    }
}
