package com.eugene.wc.protocol.identity;

import com.eugene.wc.protocol.api.db.DatabaseComponent;
import com.eugene.wc.protocol.api.db.DatabaseOpenListener;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.identity.IdentityManager;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.inject.Inject;

public class IdentityManagerImpl implements IdentityManager, DatabaseOpenListener {

    private static final Logger logger = Logger.getLogger(IdentityManagerImpl.class.getName());

    private final DatabaseComponent dbComponent;

    private volatile boolean shouldStoreIdentity;
    private Identity cachedIdentity;

    @Inject
    public IdentityManagerImpl(DatabaseComponent dbComponent) {
        this.dbComponent = dbComponent;
    }

    @Override
    public void storeIdentity(Identity identity) {
        cachedIdentity = identity;
        shouldStoreIdentity = true;
    }

    @Override
    public void onDatabaseOpened(Connection txn) {
        if (shouldStoreIdentity) {
            try {
                dbComponent.createIdentity(txn, cachedIdentity);
            } catch (DbException e) {
                logger.warning("Unable to create identity " + e);
            }
        } else {
            try {
                Identity identity = dbComponent.getIdentity(txn);
                if (identity != null) {
                    logger.info("Identity name: " + identity.getName());
                } else {
                    logger.info("Identity is null");
                }
            } catch (DbException e) {
                logger.warning("Unable to get identity " + e);
            }
        }
    }

    @Override
    public Identity getIdentity() throws DbException {
        if (cachedIdentity != null) {
            return cachedIdentity;
        }
        return dbComponent.runInTransactionWithResult(true, dbComponent::getIdentity);
    }
}
