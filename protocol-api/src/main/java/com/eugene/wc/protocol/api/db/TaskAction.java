package com.eugene.wc.protocol.api.db;

public class TaskAction implements CommitAction {

    private final Runnable runnable;

    public TaskAction(Runnable runnable) {
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
