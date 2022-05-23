package com.eugene.wc.protocol.api.keyexchange.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.keyexchange.KeyExchangeResult;

public class KeyExchangeFinishedEvent extends Event {

    private final KeyExchangeResult result;

    public KeyExchangeFinishedEvent(KeyExchangeResult result) {
        this.result = result;
    }

    public KeyExchangeResult getResult() {
        return result;
    }
}
