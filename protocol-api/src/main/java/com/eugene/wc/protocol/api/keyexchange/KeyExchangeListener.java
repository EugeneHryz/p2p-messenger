package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.data.WdfList;

import java.io.IOException;

/**
 * Accepts key agreement connections over a given transport.
 */
public abstract class KeyExchangeListener {

	private final WdfList descriptor;

	public KeyExchangeListener(WdfList descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * Returns the descriptor that a remote peer can use to connect to this
	 * listener.
	 */
	public WdfList getDescriptor() {
		return descriptor;
	}

	/**
	 * Blocks until an incoming connection is received and returns it.
	 *
	 * @throws IOException if an error occurs or {@link #close()} is called.
	 */
	public abstract KeyExchangeConnection accept() throws IOException;

	/**
	 * Closes the underlying server socket.
	 */
	public abstract void close();
}
