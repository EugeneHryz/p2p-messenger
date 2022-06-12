package com.eugene.wc.protocol.api.session;

import com.eugene.wc.protocol.api.identity.IdentityId;

public class Ack {

    private final IdentityId localId;
    private final IdentityId remoteId;

    public Ack(IdentityId localId, IdentityId remoteId) {
        this.localId = localId;
        this.remoteId = remoteId;
    }

    public IdentityId getLocalId() {
        return localId;
    }

    public IdentityId getRemoteId() {
        return remoteId;
    }
}
