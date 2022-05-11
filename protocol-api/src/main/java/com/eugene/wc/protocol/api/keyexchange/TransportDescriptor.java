package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.data.WdfList;

public class TransportDescriptor {

    private final String transportId;
    private final WdfList properties;

    public TransportDescriptor(String transportId, WdfList properties) {
        this.transportId = transportId;
        this.properties = properties;
    }

    public String getTransportId() {
        return transportId;
    }

    public WdfList getProperties() {
        return properties;
    }
}
