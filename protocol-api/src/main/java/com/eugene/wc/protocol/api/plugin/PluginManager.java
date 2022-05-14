package com.eugene.wc.protocol.api.plugin;

import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;

import java.util.Collection;

/**
 * Responsible for starting transport plugins at startup and stopping them at
 * shutdown.
 */
public interface PluginManager {

	/**
	 * Returns the plugin for the given transport, or null if no such plugin
	 * has been created.
	 */
	Plugin getPlugin(TransportId t);

	/**
	 * Returns any duplex plugins that have been created.
	 */
	Collection<DuplexPlugin> getDuplexPlugins();

	/**
	 * Returns any duplex plugins that support key agreement.
	 */
	Collection<DuplexPlugin> getKeyAgreementPlugins();


//	Collection<DuplexPlugin> getRendezvousPlugins();

	/**
	 * Enables or disables the plugin
	 * identified by the given {@link TransportId}.
	 * <p>
	 * Note that this applies the change asynchronously
	 * and there are no order guarantees.
	 * <p>
	 * If no plugin with the given {@link TransportId} is registered,
	 * this is a no-op.
	 */
	void setPluginEnabled(TransportId t, boolean enabled);

}
