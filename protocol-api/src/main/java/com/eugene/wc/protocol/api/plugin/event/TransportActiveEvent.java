package com.eugene.wc.protocol.api.plugin.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.plugin.TransportId;

public class TransportActiveEvent extends Event {

	private final TransportId transportId;

	public TransportActiveEvent(TransportId transportId) {
		this.transportId = transportId;
	}

	public TransportId getTransportId() {
		return transportId;
	}
}
