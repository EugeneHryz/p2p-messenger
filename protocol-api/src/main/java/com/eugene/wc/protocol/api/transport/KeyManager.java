package com.eugene.wc.protocol.api.transport;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.crypto.SecretKey;

public interface KeyManager {

    int TRANSPORT_DERIVE_KEY_NUMBER = 20;

    Pair<SecretKey, Tag> retrieveNextOutgoingKeyAndTag();

    SecretKey retrieveIncomingKey(Tag tag);
}
