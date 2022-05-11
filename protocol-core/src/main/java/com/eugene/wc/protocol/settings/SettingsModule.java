package com.eugene.wc.protocol.settings;

import com.eugene.wc.protocol.api.settings.SettingsManager;

import dagger.Module;
import dagger.Provides;

@Module
public class SettingsModule {

	@Provides
	SettingsManager provideSettingsManager(SettingsManagerImpl settingsManager) {
		return settingsManager;
	}
}
