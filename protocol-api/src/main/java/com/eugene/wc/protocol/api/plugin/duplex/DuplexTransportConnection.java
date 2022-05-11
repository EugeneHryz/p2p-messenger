package com.eugene.wc.protocol.api.plugin.duplex;

import com.eugene.wc.protocol.api.plugin.TransportConnectionReader;
import com.eugene.wc.protocol.api.plugin.TransportConnectionWriter;
import com.eugene.wc.protocol.api.properties.TransportProperties;


public interface DuplexTransportConnection {
	/**
	 * Returns a {@link TransportConnectionReader TransportConnectionReader}
	 * for reading from the connection.
	 */
	TransportConnectionReader getReader();

	/**
	 * Returns a {@link TransportConnectionWriter TransportConnectionWriter}
	 * for writing to the connection.
	 */
	TransportConnectionWriter getWriter();

	/**
	 * Returns a possibly empty set of {@link TransportProperties} describing
	 * the remote peer.
	 */
	TransportProperties getRemoteProperties();
}
