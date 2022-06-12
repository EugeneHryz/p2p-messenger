package com.eugene.wc.protocol.keyexchange.record;

import com.eugene.wc.protocol.api.Predicate;
import com.eugene.wc.protocol.api.keyexchange.record.Record;
import com.eugene.wc.protocol.api.keyexchange.record.RecordReader;
import com.eugene.wc.protocol.api.util.ByteUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RecordReaderImpl implements RecordReader {

    private final DataInputStream input;

    public RecordReaderImpl(InputStream in) {
        if (!in.markSupported()) in = new BufferedInputStream(in, 1);
        this.input = new DataInputStream(in);
    }

    @Override
    public Record readRecord(Predicate<Record> accept) throws IOException {
        Record recordWeNeed = null;
        while (recordWeNeed == null && !isEof()) {

            byte nextType = (byte) input.read();
            byte[] lengthBytes = new byte[ByteUtils.INT_32_BYTES];
            input.readFully(lengthBytes);
            int length = (int) ByteUtils.readUint32(lengthBytes, 0);

            byte[] content = new byte[length];
            input.readFully(content);

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

        byte[] lengthBytes = new byte[ByteUtils.INT_32_BYTES];
        input.readFully(lengthBytes);
        int length = (int) ByteUtils.readUint32(lengthBytes, 0);

        byte[] content = new byte[length];
        input.readFully(content);

        return new Record(type, content);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    private boolean isEof() throws IOException {
        boolean eof;
        input.mark(1);
        eof = input.read() == -1;
        input.reset();
        return eof;
    }
}
