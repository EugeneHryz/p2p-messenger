package com.eugene.wc.protocol;

import com.eugene.wc.protocol.account.AccountManagerModule;
import com.eugene.wc.protocol.contact.ContactModule;
import com.eugene.wc.protocol.crypto.CryptoModule;
import com.eugene.wc.protocol.db.DatabaseModule;
import com.eugene.wc.protocol.event.EventModule;
import com.eugene.wc.protocol.identity.IdentityModule;
import com.eugene.wc.protocol.io.IoExecutorModule;
import com.eugene.wc.protocol.keyexchange.KeyExchangeModule;
import com.eugene.wc.protocol.lifecycle.LifecycleModule;
import com.eugene.wc.protocol.plugin.PluginModule;
import com.eugene.wc.protocol.settings.SettingsModule;

import dagger.Module;

@Module(includes = { DatabaseModule.class,
                    AccountManagerModule.class,
                    IoExecutorModule.class,
                    CryptoModule.class,
                    LifecycleModule.class,
                    EventModule.class,
                    SettingsModule.class,
                    PluginModule.class,
                    KeyExchangeModule.class,
                    IdentityModule.class,
                    ContactModule.class})
public class ProtocolCoreModule {
}
