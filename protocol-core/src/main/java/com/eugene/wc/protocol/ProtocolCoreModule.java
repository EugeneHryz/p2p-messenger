package com.eugene.wc.protocol;

import com.eugene.wc.protocol.account.AccountManagerModule;
import com.eugene.wc.protocol.crypto.CryptoModule;
import com.eugene.wc.protocol.db.DatabaseModule;
import com.eugene.wc.protocol.io.IoExecutorModule;

import dagger.Module;

@Module(includes = { DatabaseModule.class,
                    AccountManagerModule.class,
                    IoExecutorModule.class,
                    CryptoModule.class})
public class ProtocolCoreModule {
}
