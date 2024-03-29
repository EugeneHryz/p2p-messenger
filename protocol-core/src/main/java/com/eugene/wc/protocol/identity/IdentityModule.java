package com.eugene.wc.protocol.identity;

import com.eugene.wc.protocol.api.identity.IdentityFactory;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;

import dagger.Module;
import dagger.Provides;

@Module
public class IdentityModule {

    @Provides
    public IdentityFactory provideIdentityFactory(IdentityFactoryImpl factory) {
        return factory;
    }

    @Provides
    public IdentityManager provideIdentityManager(IdentityManagerImpl identityManager,
                                                  LifecycleManager lifecycleManager) {
        lifecycleManager.registerDatabaseOpenListener(identityManager);
        return identityManager;
    }
}
