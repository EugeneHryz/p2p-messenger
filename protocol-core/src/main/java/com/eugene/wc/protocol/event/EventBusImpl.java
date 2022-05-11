package com.eugene.wc.protocol.event;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.lifecycle.EventExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import javax.inject.Inject;

public class EventBusImpl implements EventBus {

    private final Executor eventExecutor;

    private final List<EventListener> listeners;

    @Inject
    public EventBusImpl(@EventExecutor Executor executor) {
        eventExecutor = executor;

        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void broadcast(Event event) {
        eventExecutor.execute(() -> {
            for (EventListener listener : listeners) {
                listener.onEventOccurred(event);
            }
        });
    }
}
