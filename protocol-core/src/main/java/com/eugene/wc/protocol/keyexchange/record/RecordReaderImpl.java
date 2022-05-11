package com.eugene.wc.protocol.keyexchange.record;

import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.protocol.api.keyexchange.record.Record;
import com.eugene.wc.protocol.api.keyexchange.record.RecordReader;

import java.io.IOException;
import java.io.InputStream;

public class RecordReaderImpl implements RecordReader {

    private final InputStream input;

    public RecordReaderImpl(InputStream input) {
        this.input = input;
    }

    @Override
    public Record readRecord(Predicate<Record> accept) throws IOException {
        Record recordWeNeed = null;
        while (recordWeNeed == null && !isEof()) {

            byte nextType = (byte) input.read();
            int length = readInteger32();

            byte[] content = input.readNBytes(length);
            Record record = new Record(nextType, content);
            if (accept.test(record)) {
                recordWeNeed = record;
            }
        }
        return recordWeNeed;
    }

    @Override
    public Record readNextRecord() throws IOException {
        byte type = (byte) input.read();
        int length = readInteger32();
        byte[] content = input.readNBytes(length);

        return new Record(type, content);
    }

    private int readInteger32() throws IOException {
        int value = 0;
        for (int i = 24; i >= 0; i -= 8) {
            int nextByte = input.read();
            value |= (nextByte << i);
        }
        return value;
    }

    private boolean isEof() throws IOException {
        boolean eof;
        input.mark(1);
        eof = input.read() == -1;
        input.reset();
        return eof;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
