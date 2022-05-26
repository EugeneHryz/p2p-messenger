package com.eugene.wc.protocol.api.sync;

import java.util.TreeMap;

public class Metadata extends TreeMap<String, byte[]> {

	/**
	 * Special value to indicate that a key is being removed.
	 */
	public static final byte[] REMOVE = new byte[0];
}
