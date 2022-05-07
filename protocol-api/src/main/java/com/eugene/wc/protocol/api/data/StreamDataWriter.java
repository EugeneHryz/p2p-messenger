package com.eugene.wc.protocol.api.data;

import java.io.IOException;

public interface StreamDataWriter {

    void writeInteger(int value) throws IOException;

    void writeDouble(double value) throws IOException;

    void writeBoolean(boolean value) throws IOException;

    void writeString(String value) throws IOException;

    void writeRaw(byte[] value) throws IOException;

    void writeStartOfTheList() throws IOException;

    void writeEndOfTheList() throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
