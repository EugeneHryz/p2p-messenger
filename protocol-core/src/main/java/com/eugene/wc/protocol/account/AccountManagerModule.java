package com.eugene.wc.protocol.account;

import com.eugene.wc.protocol.api.account.AccountManager;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AccountManagerModule {

    @Singleton
    @Provides
    public AccountManager provideAccountManager(AccountManagerImpl accountManager) {
        return accountManager;
    }

    @Provides
    public PasswordStrengthEstimator providePasswordStrengthEstimator() {
        return new PasswordStrengthEstimatorImpl();
    }
}
