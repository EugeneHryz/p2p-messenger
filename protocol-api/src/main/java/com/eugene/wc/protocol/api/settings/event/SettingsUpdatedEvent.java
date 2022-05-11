package com.eugene.wc.protocol.api.settings.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.settings.Settings;


public class SettingsUpdatedEvent extends Event {

	private final String namespace;
	private final Settings settings;

	public SettingsUpdatedEvent(String namespace, Settings settings) {
		this.namespace = namespace;
		this.settings = settings;
	}

	public String getNamespace() {
		return namespace;
	}

	public Settings getSettings() {
		return settings;
	}
}
