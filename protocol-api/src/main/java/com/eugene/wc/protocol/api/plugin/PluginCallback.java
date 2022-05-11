package com.eugene.wc.protocol.api.plugin;

import com.eugene.wc.protocol.api.properties.TransportProperties;
import com.eugene.wc.protocol.api.settings.Settings;

import java.util.Collection;

/**
 * An interface through which a transport plugin interacts with the rest of
 * the application.
 */
public interface PluginCallback extends ConnectionHandler {

	/**
	 * Returns the plugin's settings
	 */
	Settings getSettings();

	/**
	 * Returns the plugin's local transport properties.
	 */
	TransportProperties getLocalProperties();

	/**
	 * Returns the plugin's remote transport properties.
	 */
	Collection<TransportProperties> getRemoteProperties();

	/**
	 * Merges the given settings with the plugin's settings
	 */
	void mergeSettings(Settings s);

	/**
	 * Merges the given properties with the plugin's local transport properties.
	 */
	void mergeLocalProperties(TransportProperties p);


	void pluginStateChanged(Plugin.State state);
}
