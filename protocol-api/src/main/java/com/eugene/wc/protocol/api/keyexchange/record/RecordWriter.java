package com.eugene.wc.protocol.api.keyexchange.record;

import java.io.IOException;

public interface RecordWriter {

    void writeRecord(Record record) throws IOException;

    void flush() throws IOException;

    void close() throws IOException;
}
