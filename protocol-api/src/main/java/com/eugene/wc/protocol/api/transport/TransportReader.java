package com.eugene.wc.protocol.api.transport;

import java.io.IOException;

public interface TransportReader {

    EncryptedPacket readNextPacket() throws IOException;

    void close() throws IOException;

    // check eof method?
}
