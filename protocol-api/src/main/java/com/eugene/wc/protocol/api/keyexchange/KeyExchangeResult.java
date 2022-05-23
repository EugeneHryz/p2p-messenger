package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.crypto.SecretKey;

public class KeyExchangeResult {

    private final boolean isAlice;
    private final KeyExchangeConnection transport;
    private final SecretKey masterKey;

    public KeyExchangeResult(boolean isAlice, KeyExchangeConnection transport, SecretKey masterKey) {
        this.isAlice = isAlice;
        this.transport = transport;
        this.masterKey = masterKey;
    }

    public boolean isAlice() {
        return isAlice;
    }

    public KeyExchangeConnection getTransport() {
        return transport;
    }

    public SecretKey getMasterKey() {
        return masterKey;
    }
}
