package com.eugene.wc.protocol.keyexchange.record;

import com.eugene.wc.protocol.api.keyexchange.record.Record;
import com.eugene.wc.protocol.api.keyexchange.record.RecordWriter;
import com.eugene.wc.protocol.api.util.ByteUtils;

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

        byte[] lengthBytes = new byte[ByteUtils.INT_32_BYTES];
        ByteUtils.writeUint32(length, lengthBytes, 0);
        output.write(lengthBytes);
        output.write(content);
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
