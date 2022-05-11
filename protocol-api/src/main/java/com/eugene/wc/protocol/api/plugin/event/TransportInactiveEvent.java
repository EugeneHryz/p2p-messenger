package com.eugene.wc.protocol.api.plugin.event;


import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.plugin.TransportId;

public class TransportInactiveEvent extends Event {

	private final TransportId transportId;

	public TransportInactiveEvent(TransportId transportId) {
		this.transportId = transportId;
	}

	public TransportId getTransportId() {
		return transportId;
	}
}
