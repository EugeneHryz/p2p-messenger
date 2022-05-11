package com.eugene.wc.protocol.api.keyexchange.record;

import com.eugene.wc.protocol.api.Predicate;

import java.io.IOException;

public interface RecordReader {

    Record readRecord(Predicate<Record> accept) throws IOException;

    Record readNextRecord() throws IOException;

    void close() throws IOException;
}
