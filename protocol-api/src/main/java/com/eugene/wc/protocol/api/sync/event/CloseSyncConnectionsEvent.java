package com.eugene.wc.protocol.api.sync.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.plugin.TransportId;

public class CloseSyncConnectionsEvent extends Event {

	private final TransportId transportId;

	public CloseSyncConnectionsEvent(TransportId transportId) {
		this.transportId = transportId;
	}

	public TransportId getTransportId() {
		return transportId;
	}
}
