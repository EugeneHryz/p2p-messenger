package com.eugene.wc;

import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.network.AndroidNetworkModule;
import com.eugene.wc.protocol.ProtocolCoreModule;
import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.crypto.CryptoExecutor;
import com.eugene.wc.protocol.api.db.DbExecutor;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.io.IoExecutor;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeTask;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager;
import com.eugene.wc.protocol.api.plugin.PluginManager;
import com.eugene.wc.protocol.api.system.AndroidExecutor;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.api.system.Clock;
import com.eugene.wc.protocol.db.JdbcDatabase;
import com.eugene.wc.system.AndroidMessengerModule;
import com.eugene.wc.system.ClockModule;
import com.eugene.wc.viewmodel.ViewModelModule;

import java.util.concurrent.Executor;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { ProtocolCoreModule.class,
                        ViewModelModule.class,
                        AppModule.class,
                        AndroidMessengerModule.class,
                        AndroidNetworkModule.class,
                        ClockModule.class})
public interface ApplicationComponent extends AndroidEagerSingletons {

    JdbcDatabase jdbcDatabase();

    AccountManager accountManager();

    CryptoComponent cryptoComponent();

    @CryptoExecutor
    Executor cryptoExecutor();

    KeyExchangeTask keyExchangeTask();

    ViewModelProvider.Factory viewModelFactory();

    @DbExecutor
    Executor databaseExecutor();

    LifecycleManager lifecycleManager();

    PluginManager pluginManager();

    EventBus eventBus();

    AndroidExecutor androidExecutor();

    Clock clock();

    @IoExecutor
    Executor ioExecutor();

    AndroidWakeLockManager wakeLockManager();

}
