package com.eugene.wc;

import androidx.lifecycle.ViewModelProvider;

import com.eugene.wc.protocol.ProtocolCoreModule;
import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.db.JdbcDatabase;
import com.eugene.wc.viewmodel.ViewModelModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { ProtocolCoreModule.class,
                        ViewModelModule.class,
                        AppModule.class })
public interface ApplicationComponent {

    JdbcDatabase jdbcDatabase();

    AccountManager accountManager();

    ViewModelProvider.Factory viewModelFactory();
}
