package com.eugene.wc.protocol.api.contact;

import com.eugene.wc.protocol.api.crypto.PublicKey;

public class Contact {

    private int id;
    private String name;

    private String alias;
    private PublicKey publicKey;

    public Contact() {
    }

    public Contact(String name, String alias, PublicKey publicKey) {
        this.name = name;
        this.alias = alias;
        this.publicKey = publicKey;
    }

    public Contact(String name, PublicKey publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
