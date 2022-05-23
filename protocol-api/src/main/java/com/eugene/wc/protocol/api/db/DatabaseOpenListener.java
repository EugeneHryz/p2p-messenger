package com.eugene.wc.protocol.api.db;

import java.sql.Connection;

public interface DatabaseOpenListener {

    void onDatabaseOpened(Connection txn);
}
