package com.eugene.wc.protocol.api.db;

import com.eugene.wc.protocol.api.event.Event;

public class EventAction implements CommitAction {

    private final Event event;

    public EventAction(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
