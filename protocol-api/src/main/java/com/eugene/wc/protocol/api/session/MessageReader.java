package com.eugene.wc.protocol.api.session;

import com.eugene.wc.protocol.api.crypto.exception.DecryptionException;
import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.KeyManager;

import java.io.IOException;

public interface MessageReader {

    boolean hasAck() throws IOException, DecryptionException;

    boolean hasMessage() throws IOException, DecryptionException;

    boolean isEndOfSession() throws IOException, DecryptionException;

    Message readNextMessage() throws IOException, DecryptionException;

    Ack readNextAck() throws IOException, DecryptionException;

    void close() throws IOException;
}
