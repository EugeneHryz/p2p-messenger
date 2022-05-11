package com.eugene.wc.protocol.api.plugin;

import com.eugene.wc.protocol.api.plugin.duplex.DuplexPluginFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PluginConfig {

	Collection<DuplexPluginFactory> getDuplexFactories();

	boolean shouldPoll();

	/**
	 * Returns a map representing transport preferences. For each entry in the
	 * map, connections via the transports identified by the value are
	 * preferred to connections via the transport identified by the key.
	 */
	Map<TransportId, List<TransportId>> getTransportPreferences();
}
