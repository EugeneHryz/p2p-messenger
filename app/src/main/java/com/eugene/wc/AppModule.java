package com.eugene.wc;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.eugene.wc.plugin.bluetooth.AndroidBluetoothPluginFactory;
import com.eugene.wc.plugin.tcp.AndroidLanTcpPluginFactory;
import com.eugene.wc.protocol.ProtocolComponent;
import com.eugene.wc.protocol.api.db.DatabaseConfig;
import com.eugene.wc.protocol.api.plugin.BluetoothConstants;
import com.eugene.wc.protocol.api.plugin.LanTcpConstants;
import com.eugene.wc.protocol.api.plugin.PluginConfig;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexPluginFactory;
import com.eugene.wc.system.AndroidSystemModule;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = { AndroidSystemModule.class })
public class AppModule {

    private final static String DB_DIR = "db";
    private final static String DB_KEY_DIR = "db.key";

    private final Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public Application provideApplication() {
        return application;
    }

    @Provides
    @Singleton
    public ProtocolComponent provideProtocolComponent(Application application) {
        return ((MessengerApplication) application).getApplicationComponent();
    }

    @Singleton
    @Provides
    DatabaseConfig provideDatabaseConfig() {
        StrictMode.ThreadPolicy tp = StrictMode.allowThreadDiskWrites();
        StrictMode.allowThreadDiskReads();

        File dbDir = application.getApplicationContext().getDir(DB_DIR, Context.MODE_PRIVATE);
        File dbKeyDir = application.getApplicationContext().getDir(DB_KEY_DIR, Context.MODE_PRIVATE);

        StrictMode.setThreadPolicy(tp);
        return new DatabaseConfig(dbKeyDir, dbDir);
    }

    @Provides
    @Singleton
    PluginConfig providePluginConfig(AndroidBluetoothPluginFactory bluetooth,
                                     AndroidLanTcpPluginFactory lanTcp) {
        PluginConfig pluginConfig = new PluginConfig() {

            @Override
            public Collection<DuplexPluginFactory> getDuplexFactories() {
                return asList(bluetooth, lanTcp);
            }

            @Override
            public boolean shouldPoll() {
                return true;
            }

            @Override
            public Map<TransportId, List<TransportId>> getTransportPreferences() {
                // Prefer LAN to Bluetooth
                return singletonMap(BluetoothConstants.ID,
                        singletonList(LanTcpConstants.ID));
            }
        };
        return pluginConfig;
    }
}
