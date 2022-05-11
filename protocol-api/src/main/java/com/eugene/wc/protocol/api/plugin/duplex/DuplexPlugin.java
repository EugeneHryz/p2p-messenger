package com.eugene.wc.protocol.api.plugin.duplex;

import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.properties.TransportProperties;

public interface DuplexPlugin extends Plugin {

	/**
	 * Attempts to create and return a connection using the given transport
	 * properties. Returns null if a connection cannot be created.
	 */
	DuplexTransportConnection createConnection(TransportProperties p);

	/**
	 * Returns true if the plugin supports short-range key agreement.
	 */
	boolean supportsKeyAgreement();

	/**
	 * Attempts to create and return a listener that can be used to perform key
	 * agreement. Returns null if a listener cannot be created.
	 */
//	@Nullable
//	KeyAgreementListener createKeyAgreementListener(byte[] localCommitment);

	/**
	 * Attempts to connect to the remote peer specified in the given descriptor.
	 * Returns null if no connection can be established.
	 */
//	@Nullable
//	DuplexTransportConnection createKeyAgreementConnection(
//			byte[] remoteCommitment, BdfList descriptor);

	/**
	 * Returns true if the plugin supports rendezvous connections.
	 */
	boolean supportsRendezvous();

	/**
	 * Creates and returns an endpoint that uses the given key material to
	 * rendezvous with a pending contact, and the given connection handler to
	 * handle incoming connections. Returns null if an endpoint cannot be
	 * created.
	 */
//	@Nullable
//	RendezvousEndpoint createRendezvousEndpoint(KeyMaterialSource k,
//			boolean alice, ConnectionHandler incoming);
}
