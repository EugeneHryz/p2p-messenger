package com.eugene.wc.plugin.bluetooth;

import static com.eugene.wc.protocol.api.plugin.BluetoothConstants.PROP_ADDRESS;
import static com.eugene.wc.util.AndroidUtils.isValidBluetoothAddress;

import android.bluetooth.BluetoothSocket;

import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.duplex.AbstractDuplexTransportConnection;
import com.eugene.wc.protocol.api.system.AndroidWakeLock;
import com.eugene.wc.protocol.api.system.AndroidWakeLockManager;
import com.eugene.wc.protocol.plugin.bluetooth.BluetoothConnectionLimiter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class AndroidBluetoothTransportConnection extends AbstractDuplexTransportConnection {

	private final BluetoothConnectionLimiter connectionLimiter;
	private final BluetoothSocket socket;
	private final InputStream in;
	private final AndroidWakeLock wakeLock;

	AndroidBluetoothTransportConnection(Plugin plugin,
										BluetoothConnectionLimiter connectionLimiter,
										AndroidWakeLockManager wakeLockManager,
										BluetoothSocket socket) throws IOException {
		super(plugin);
		this.connectionLimiter = connectionLimiter;
		this.socket = socket;
		in = socket.getInputStream();
		wakeLock = wakeLockManager.createWakeLock("BluetoothConnection");
		wakeLock.acquire();
		String address = socket.getRemoteDevice().getAddress();
		if (isValidBluetoothAddress(address)) remote.put(PROP_ADDRESS, address);
	}

	@Override
	protected InputStream getInputStream() {
		return in;
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	protected void closeConnection(boolean exception) throws IOException {
		try {
			socket.close();
			in.close();
		} finally {
			wakeLock.release();
			connectionLimiter.connectionClosed(this);
		}
	}
}
