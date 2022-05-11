package com.eugene.wc.protocol.plugin.tcp;

import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.duplex.AbstractDuplexTransportConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
class TcpTransportConnection extends AbstractDuplexTransportConnection {

	private final Socket socket;

	TcpTransportConnection(Plugin plugin, Socket socket) {
		super(plugin);
		this.socket = socket;
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	protected void closeConnection(boolean exception) throws IOException {
		socket.close();
	}
}
