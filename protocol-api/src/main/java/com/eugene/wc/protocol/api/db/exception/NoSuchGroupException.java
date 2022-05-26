package com.eugene.wc.protocol.api.db.exception;

/**
 * Thrown when a database operation is attempted for a group that is not in the
 * database. This exception may occur due to concurrent updates and does not
 * indicate a database error.
 */
public class NoSuchGroupException extends DbException {
}
