package com.eugene.wc.protocol.api.plugin.event;


import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.TransportId;

public class TransportStateEvent extends Event {

	private final TransportId transportId;
	private final Plugin.State state;

	public TransportStateEvent(TransportId transportId, Plugin.State state) {
		this.transportId = transportId;
		this.state = state;
	}

	public TransportId getTransportId() {
		return transportId;
	}

	public Plugin.State getState() {
		return state;
	}
}
