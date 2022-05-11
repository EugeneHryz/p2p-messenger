package com.eugene.wc.protocol.api.lifecycle.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.lifecycle.LifecycleManager.State;

public class LifecycleStateEvent extends Event {

    private final State state;

    public LifecycleStateEvent(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }
}
