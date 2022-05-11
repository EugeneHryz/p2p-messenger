package com.eugene.wc.protocol.api.plugin;

public interface BluetoothConstants {

	TransportId ID = new TransportId("com.eugene.wc.protocol.bluetooth");

	int UUID_BYTES = 16;

	// Transport properties
	String PROP_ADDRESS = "address";
	String PROP_UUID = "uuid";

	// Local settings (not shared with contacts)
	String PREF_ADDRESS_IS_REFLECTED = "addressIsReflected";
	String PREF_EVER_CONNECTED = "everConnected";

	// Default values for local settings
	boolean DEFAULT_PREF_PLUGIN_ENABLE = true;
	boolean DEFAULT_PREF_ADDRESS_IS_REFLECTED = false;
	boolean DEFAULT_PREF_EVER_CONNECTED = true;
}
