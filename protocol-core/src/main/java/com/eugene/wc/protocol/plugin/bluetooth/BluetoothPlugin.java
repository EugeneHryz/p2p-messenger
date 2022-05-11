package com.eugene.wc.protocol.plugin.bluetooth;

import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

import javax.annotation.Nullable;

public interface BluetoothPlugin extends DuplexPlugin {

	boolean isDiscovering();

	void disablePolling();

	void enablePolling();

	@Nullable
	DuplexTransportConnection discoverAndConnectForSetup(String uuid);

	void stopDiscoverAndConnect();
}
