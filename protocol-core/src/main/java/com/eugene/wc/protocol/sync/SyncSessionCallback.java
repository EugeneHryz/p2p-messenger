package com.eugene.wc.protocol.sync;

public interface SyncSessionCallback {

    void onRemoteIdReceived(byte[] idBytes);
}
