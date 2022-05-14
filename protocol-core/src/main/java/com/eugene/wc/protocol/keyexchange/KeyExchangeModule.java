package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.keyexchange.ConnectionChooser;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;

import dagger.Module;
import dagger.Provides;

@Module
public class KeyExchangeModule {

    @Provides
    public ConnectionChooser provideConnectionChooser(ConnectionChooserImpl connChooser) {
        return connChooser;
    }

    @Provides
    public KeyExchangeTask provideKeyExchangeTask(KeyExchangeTaskImpl ketImpl) {
        return ketImpl;
    }
}
