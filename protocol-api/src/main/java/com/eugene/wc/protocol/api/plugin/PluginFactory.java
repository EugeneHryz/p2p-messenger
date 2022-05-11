package com.eugene.wc.protocol.api.plugin;


public interface PluginFactory<P extends Plugin> {

	/**
	 * Returns the plugin's transport identifier.
	 */
	TransportId getId();

	/**
	 * Returns the maximum latency of the transport in milliseconds.
	 */
	long getMaxLatency();

	/**
	 * Creates and returns a plugin, or null if no plugin can be created.
	 */
	P createPlugin(PluginCallback callback);
}
