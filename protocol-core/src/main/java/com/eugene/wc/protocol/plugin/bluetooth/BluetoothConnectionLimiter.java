package com.eugene.wc.protocol.plugin.bluetooth;


import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

public interface BluetoothConnectionLimiter {

	/**
	 * Tells the limiter to not allow regular polling connections (because we
	 * are about to do key agreement, or connect via BT for setup).
	 */
	void startLimiting();

	/**
	 * Tells the limiter to no longer limit regular polling connections.
	 */
	void endLimiting();

	/**
	 * Returns true if a contact connection can be opened. This method does not
	 * need to be called for key agreement connections.
	 */
	boolean canOpenContactConnection();

	/**
	 * Informs the limiter that the given connection has been opened.
	 */
	void connectionOpened(DuplexTransportConnection conn);

	/**
	 * Informs the limiter that the given connection has been closed.
	 */
	void connectionClosed(DuplexTransportConnection conn);

	/**
	 * Informs the limiter that all connections have been closed.
	 */
	void allConnectionsClosed();
}
