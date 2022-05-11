package com.eugene.wc.protocol.api.plugin;

import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

public interface ConnectionHandler {

	void handleConnection(DuplexTransportConnection c);

	void handleReader(TransportConnectionReader r);

	void handleWriter(TransportConnectionWriter w);
}
