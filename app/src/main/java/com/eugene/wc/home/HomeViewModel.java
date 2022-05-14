package com.eugene.wc.home;

import static com.eugene.wc.protocol.api.plugin.Plugin.State.STARTING_STOPPING;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eugene.wc.protocol.api.event.Event;
import com.eugene.wc.protocol.api.event.EventBus;
import com.eugene.wc.protocol.api.event.EventListener;
import com.eugene.wc.protocol.api.plugin.BluetoothConstants;
import com.eugene.wc.protocol.api.plugin.LanTcpConstants;
import com.eugene.wc.protocol.api.plugin.Plugin;
import com.eugene.wc.protocol.api.plugin.PluginManager;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.event.TransportStateEvent;

import javax.inject.Inject;

public class HomeViewModel extends ViewModel implements EventListener {

    private final PluginManager pluginManager;
    private final EventBus eventBus;

    private final MutableLiveData<Plugin.State> bluetoothState = new MutableLiveData<>();
    private final MutableLiveData<Plugin.State> lanState = new MutableLiveData<>();

    @Inject
    public HomeViewModel(PluginManager pluginManager, EventBus eventBus) {
        this.pluginManager = pluginManager;
        this.eventBus = eventBus;
        bluetoothState.setValue(getTransportState(BluetoothConstants.ID));
        lanState.setValue(getTransportState(LanTcpConstants.ID));

        eventBus.addListener(this);
    }

    @Override
    protected void onCleared() {
        eventBus.removeListener(this);
    }

    @Override
    public void onEventOccurred(Event e) {
        if (e instanceof TransportStateEvent) {
            TransportStateEvent event = (TransportStateEvent) e;
            updatePluginState(event);
        }
    }

    private void updatePluginState(TransportStateEvent event) {
        if (event.getTransportId().equals(BluetoothConstants.ID)) {
            bluetoothState.postValue(event.getState());

        } else if (event.getTransportId().equals(LanTcpConstants.ID)) {
            lanState.postValue(event.getState());
        }
    }

    private Plugin.State getTransportState(TransportId id) {
        Plugin plugin = pluginManager.getPlugin(id);
        return plugin == null ? STARTING_STOPPING : plugin.getState();
    }

    public LiveData<Plugin.State> getBluetoothState() {
        return bluetoothState;
    }

    public LiveData<Plugin.State> getLanState() {
        return lanState;
    }
}
