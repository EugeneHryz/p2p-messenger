package com.eugene.wc.protocol.api.db;

import com.eugene.wc.protocol.api.db.exception.DbException;

import java.sql.Connection;

@FunctionalInterface
public interface DbRunnable<E extends Exception> {

    void run(Connection txn) throws DbException, E;
}
