package com.eugene.wc.protocol.api.contact;

import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;

public class Contact {

    private final ContactId id;
    private final IdentityId localIdentityId;
    private final Identity identity;

    public Contact(ContactId id, IdentityId localIdentityId, Identity identity) {
        this.id = id;
        this.localIdentityId = localIdentityId;
        this.identity = identity;
    }

    public ContactId getId() {
        return id;
    }

    public IdentityId getLocalIdentityId() {
        return localIdentityId;
    }

    public Identity getIdentity() {
        return identity;
    }
}
