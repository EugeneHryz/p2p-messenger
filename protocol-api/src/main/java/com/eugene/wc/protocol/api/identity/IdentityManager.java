package com.eugene.wc.protocol.api.identity;

import com.eugene.wc.protocol.api.db.exception.DbException;

public interface IdentityManager {

    void storeIdentity(Identity identity);

    // there supposed to be only one identity associated with device
    Identity getIdentity() throws DbException;
}
