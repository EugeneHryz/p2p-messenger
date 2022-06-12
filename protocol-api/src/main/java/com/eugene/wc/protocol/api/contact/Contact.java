package com.eugene.wc.protocol.api.contact;

import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityId;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Contact {

    private final ContactId id;
    private final IdentityId localIdentityId;
    private final Identity identity;
    private final LocalDate addedDate;

    public Contact(ContactId id, IdentityId localIdentityId, Identity identity,
                   LocalDate addedDate) {
        this.id = id;
        this.localIdentityId = localIdentityId;
        this.identity = identity;
        this.addedDate = addedDate;
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

    public LocalDate getAddedDate() {
        return addedDate;
    }
}
