package com.eugene.wc.protocol.api.session;

import com.eugene.wc.protocol.api.crypto.exception.CryptoException;

import java.io.IOException;

public interface MessageWriter {

    void sendMessage(Message message) throws IOException, CryptoException;

    void sendAck(Ack ack) throws IOException, CryptoException;

    void sendEndOfSession() throws IOException, CryptoException;

    void close() throws IOException;
}
