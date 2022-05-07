package com.eugene.wc.protocol.account;

import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;
import com.eugene.wc.protocol.api.crypto.CryptoComponent;
import com.eugene.wc.protocol.api.db.DatabaseConfig;

import dagger.Module;
import dagger.Provides;

@Module
public class AccountManagerModule {

    @Provides
    public AccountManager provideAccountManager(DatabaseConfig dbConfig, CryptoComponent crypto) {
        return new AccountManagerImpl(dbConfig, crypto);
    }

    @Provides
    public PasswordStrengthEstimator providePasswordStrengthEstimator() {
        return new PasswordStrengthEstimatorImpl();
    }
}
