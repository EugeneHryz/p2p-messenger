package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.settings.Settings;

import java.sql.Connection;

public interface JdbcDatabase {

    boolean open(SecretKey key) throws DbException;

    void close() throws DbException;

    Connection startTransaction() throws DbException;

    void commitTransaction(Connection txn) throws DbException;

    void abortTransaction(Connection txn);


    Settings getSettings(Connection txn, String namespace) throws DbException;

    void mergeSettings(Connection txn, Settings settings, String namespace) throws DbException;
}
