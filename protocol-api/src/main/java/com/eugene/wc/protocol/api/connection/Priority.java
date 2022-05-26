package com.eugene.wc.protocol.api.connection;

/**
 * A record containing a nonce for choosing between redundant sessions.
 */
public class Priority {

	private final byte[] nonce;

	public Priority(byte[] nonce) {
		this.nonce = nonce;
	}

	public byte[] getNonce() {
		return nonce;
	}
}
