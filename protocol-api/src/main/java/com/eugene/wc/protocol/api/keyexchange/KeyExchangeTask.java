package com.eugene.wc.protocol.api.keyexchange;

import com.eugene.wc.protocol.api.crypto.KeyPair;

public interface KeyExchangeTask {

    // this needs to be called to obtain descriptors for available transports
    // and after that we will be able to display qr code
    void listen(KeyPair ephemeralKeyPair);

    void stopListening();

    // this needs to be called after scanning qr code of a remote peer
    void connectAndPerformKeyExchange(Payload remotePayload);

}
