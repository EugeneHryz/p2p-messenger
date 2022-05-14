package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.keyexchange.Payload;

// todo: temp class
public class ReceivedLocalPayloadEvent extends Event {

    private final Payload localPayload;

    public ReceivedLocalPayloadEvent(Payload localPayload) {
        this.localPayload = localPayload;
    }

    public Payload getLocalPayload() {
        return localPayload;
    }
}
