package com.eugene.wc;

import com.eugene.wc.network.AndroidNetworkModule;
import com.eugene.wc.protocol.contact.ContactModule;
import com.eugene.wc.protocol.plugin.PluginModule;
import com.eugene.wc.protocol.properties.PropertiesModule;
import com.eugene.wc.system.AndroidTaskSchedulerModule;

public interface AndroidEagerSingletons {

    void inject(AndroidNetworkModule.EagerSingletons singletons);

    void inject(AndroidTaskSchedulerModule.EagerSingletons singletons);

    void inject(PropertiesModule.EagerSingletons singletons);

    void inject(PluginModule.EagerSingletons singletons);

    void inject(ContactModule.EagerSingletons singletons);

    class Helper {

        public static void injectEagerSingletons(AndroidEagerSingletons a) {
            a.inject(new AndroidNetworkModule.EagerSingletons());
            a.inject(new AndroidTaskSchedulerModule.EagerSingletons());
            a.inject(new PropertiesModule.EagerSingletons());
            a.inject(new PluginModule.EagerSingletons());
            a.inject(new ContactModule.EagerSingletons());
        }
    }
}
