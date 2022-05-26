package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;

public class LocalIdentity extends Identity {

    private PrivateKey privateKey;

    public LocalIdentity(IdentityId id, PublicKey publicKey, String name, PrivateKey key) {
        super(id, publicKey, name);

        privateKey = key;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
