package com.eugene.wc.protocol.api.db;

import com.eugene.wc.protocol.api.db.exception.DbException;

import java.sql.Connection;

@FunctionalInterface
public interface DbCallable<R, E extends Exception> {

    R call(Connection txn) throws DbException, E;
}
