package com.eugene.wc.protocol.session;

import com.eugene.wc.protocol.api.session.Message;

public interface SessionReaderCallback {

    void onMessageReceived(Message message);
}
