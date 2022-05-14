package com.eugene.wc.protocol.api.keyexchange;

import java.util.concurrent.Callable;

public interface ConnectionChooser {

    void submitTask(Callable<KeyExchangeConnection> connectionTask);

    KeyExchangeConnection pollConnection(long timeout);

    void close();
}
