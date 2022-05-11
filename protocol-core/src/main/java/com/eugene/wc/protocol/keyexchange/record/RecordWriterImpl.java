package com.eugene.wc.protocol.keyexchange.record;

import com.eugene.wc.protocol.api.keyexchange.record.Record;
import com.eugene.wc.protocol.api.keyexchange.record.RecordWriter;

import java.io.IOException;
import java.io.OutputStream;

public class RecordWriterImpl implements RecordWriter {

    private final OutputStream output;

    public RecordWriterImpl(OutputStream output) {
        this.output = output;
    }

    @Override
    public void writeRecord(Record record) throws IOException {
        output.write(record.getType());
        byte[] content = record.getContent();
        int length = content.length;
        writeInteger32(length);
        output.write(content);
    }

    private void writeInteger32(int value) throws IOException {
        for (int i = 24; i >= 0; i -= 8) {
            output.write(value >> i);
        }
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
