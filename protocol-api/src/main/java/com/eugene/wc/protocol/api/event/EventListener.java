package com.eugene.wc.protocol.api.event;

@FunctionalInterface
public interface EventListener {

    void onEventOccurred(Event e);
}
