package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.plugin.TransportId;

public class TransportDescriptor {

    private final TransportId transportId;
    private final WdfList properties;

    public TransportDescriptor(TransportId transportId, WdfList properties) {
        this.transportId = transportId;
        this.properties = properties;
    }

    public TransportId getTransportId() {
        return transportId;
    }

    public WdfList getProperties() {
        return properties;
    }
}
