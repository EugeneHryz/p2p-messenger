package com.eugene.wc.protocol.api.db;

public interface CommitAction {

    void accept(Visitor visitor);

    interface Visitor {

        void visit(EventAction eventAction);

        void visit(TaskAction taskAction);
    }
}
