package com.eugene.wc.protocol.api.transport;

import java.io.IOException;

public interface TransportWriter {

    void writePacket(EncryptedPacket packet) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
