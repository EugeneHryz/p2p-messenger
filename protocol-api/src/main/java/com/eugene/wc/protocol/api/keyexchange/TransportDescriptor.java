package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.plugin.TransportId;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransportDescriptor that = (TransportDescriptor) o;
        return Objects.equals(transportId, that.transportId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transportId);
    }

    @Override
    public String toString() {
        return "TransportDescriptor{" +
                "transportId=" + transportId +
                '}';
    }
}
