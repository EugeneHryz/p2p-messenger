package com.eugene.wc.plugin.bluetooth;

import android.bluetooth.BluetoothSocket;

import com.eugene.wc.protocol.api.plugin.duplex.DuplexPlugin;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionFactory;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionLimiter;

import java.io.IOException;

class AndroidBluetoothConnectionFactory implements BluetoothConnectionFactory<BluetoothSocket> {

	private final BluetoothConnectionLimiter connectionLimiter;
	private final AndroidWakeLockManager wakeLockManager;
//	private final TimeoutMonitor timeoutMonitor;

	AndroidBluetoothConnectionFactory(
			BluetoothConnectionLimiter connectionLimiter,
			AndroidWakeLockManager wakeLockManager) {
		this.connectionLimiter = connectionLimiter;
		this.wakeLockManager = wakeLockManager;
	}

	@Override
	public DuplexTransportConnection wrapSocket(DuplexPlugin plugin,
												BluetoothSocket s) throws IOException {
		return new AndroidBluetoothTransportConnection(plugin, connectionLimiter, wakeLockManager, s);
	}
}
