package com.eugene.wc.protocol.api.keyexchange.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.keyexchange.Payload;

public class KeyExchangeListeningEvent extends Event {

    private final Payload payload;

    public KeyExchangeListeningEvent(Payload payload) {
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }
}
