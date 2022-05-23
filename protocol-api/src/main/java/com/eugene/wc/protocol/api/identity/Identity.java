package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.crypto.PrivateKey;
import com.eugene.wc.protocol.api.crypto.PublicKey;

// do we need separate class without private key
public class Identity {

    private final PublicKey publicKey;
    private final String name;

    private PrivateKey privateKey;

    public Identity(PublicKey publicKey, PrivateKey privateKey, String name) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.name = name;
    }

    public Identity(PublicKey publicKey, String name) {
        this.publicKey = publicKey;
        this.name = name;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getName() {
        return name;
    }
}
