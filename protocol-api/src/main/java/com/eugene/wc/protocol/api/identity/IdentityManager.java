package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.db.exception.DbException;

public interface IdentityManager {

    void createIdentity(String name);

    // there supposed to be only one identity associated with device
    LocalIdentity getIdentity() throws DbException;
}
