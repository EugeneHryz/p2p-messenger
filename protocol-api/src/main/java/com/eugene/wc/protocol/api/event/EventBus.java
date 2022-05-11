package com.eugene.wc.protocol.api.event;

public interface EventBus {

    void addListener(EventListener listener);

    void removeListener(EventListener listener);

    void broadcast(Event event);
}
