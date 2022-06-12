package com.eugene.wc.protocol.identity;

import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.DatabaseOpenListener;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityFactory;
import com.eugene.wc.protocol.api.identity.IdentityManager;
import com.eugene.wc.protocol.api.identity.LocalIdentity;
import com.eugene.wc.protocol.api.transport.TransportKeys;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.inject.Inject;

public class IdentityManagerImpl implements IdentityManager, DatabaseOpenListener {

    private static final Logger logger = Logger.getLogger(IdentityManagerImpl.class.getName());

    private final DatabaseComponent dbComponent;
    private final IdentityFactory identityFactory;

    private volatile boolean shouldStoreIdentity;
    private LocalIdentity cachedIdentity;

    @Inject
    public IdentityManagerImpl(DatabaseComponent dbComponent, IdentityFactory identityFactory) {
        this.dbComponent = dbComponent;
        this.identityFactory = identityFactory;
    }

    @Override
    public void createIdentity(String name) {
        cachedIdentity = identityFactory.createLocalIdentity(name);
        shouldStoreIdentity = true;
    }

    @Override
    public void onDatabaseOpened(Connection txn) {
        if (shouldStoreIdentity) {
            try {
                dbComponent.createLocalIdentity(txn, cachedIdentity);
            } catch (DbException e) {
                logger.warning("Unable to create local identity " + e);
            }
        }
    }

    @Override
    public LocalIdentity getIdentity() throws DbException {
        if (cachedIdentity != null) {
            return cachedIdentity;
        }
        return dbComponent.runInTransactionWithResult(true, dbComponent::getLocalIdentity);
    }
}
