package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.api.plugin.duplex.DuplexTransportConnection;

public class KeyExchangeConnection {

    private final DuplexTransportConnection connection;
    private final TransportId transportId;

    public KeyExchangeConnection(DuplexTransportConnection connection, TransportId transportId) {
        this.connection = connection;
        this.transportId = transportId;
    }

    public DuplexTransportConnection getConnection() {
        return connection;
    }

    public TransportId getTransportId() {
        return transportId;
    }
}
