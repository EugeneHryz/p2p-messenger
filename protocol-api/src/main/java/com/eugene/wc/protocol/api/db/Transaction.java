package com.eugene.wc.protocol.api.db;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Transaction {

    private Connection connection;

    private final List<CommitAction> commitActions = new ArrayList<>();

    public Transaction(Connection connection) {
        this.connection = connection;
    }

    public void attachAction(CommitAction action) {
        commitActions.add(action);
    }

    public Connection getConnection() {
        return connection;
    }

    public List<CommitAction> getCommitActions() {
        return new ArrayList<>(commitActions);
    }
}
