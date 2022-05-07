package com.eugene.wc.protocol.api.data;

import java.io.IOException;

public interface StreamDataReader {

    int readNextInt() throws IOException;

    double readNextDouble() throws IOException;

    String readNextString() throws IOException;

    boolean readNextBoolean() throws IOException;

    byte[] readNextRaw() throws IOException;

    void readListStart() throws IOException;

    void readListEnd() throws IOException;

    void close() throws IOException;
}
