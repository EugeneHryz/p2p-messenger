package com.eugene.wc.protocol.api.contact.exchange;

import com.eugene.wc.protocol.api.Pair;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.transport.Tag;

public interface ContactExchangeKeyManager {

    void generateInitialKeys(SecretKey key);

    Pair<SecretKey, Tag> retrieveNextOutgoingKeyAndTag();

    SecretKey retrieveIncomingKey(Tag tag);

}
