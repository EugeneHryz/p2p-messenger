package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.crypto.PublicKey;

public class Identity {

    private IdentityId id;
    private PublicKey publicKey;
    private String name;

    public Identity(IdentityId id, PublicKey publicKey, String name) {
        this.id = id;
        this.publicKey = publicKey;
        this.name = name;
    }

    public IdentityId getId() {
        return id;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getName() {
        return name;
    }

    public void setId(IdentityId id) {
        this.id = id;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void setName(String name) {
        this.name = name;
    }
}
