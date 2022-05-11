package com.eugene.wc.protocol.api.network.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.network.NetworkStatus;

public class NetworkStatusEvent extends Event {

	private final NetworkStatus status;

	public NetworkStatusEvent(NetworkStatus status) {
		this.status = status;
	}

	public NetworkStatus getStatus() {
		return status;
	}
}