package com.eugene.wc.protocol.data;

import static com.eugene.wc.protocol.api.data.TypesDefinition.*;

import com.eugene.wc.protocol.api.data.StreamDataWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StreamDataWriterImpl implements StreamDataWriter {

    // sizes are in bytes
    private static final int LONG_SIZE = DOUBLE_SIZE;

    private final OutputStream out;

    public StreamDataWriterImpl(OutputStream out) {
        this.out = out;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void writeInteger(int value) throws IOException {
        out.write(INTEGER_TYPE);
        for (int i = (INTEGER_SIZE - 1) * 8; i >= 0; i -= 8) {
            out.write(value >> i);
        }
    }

    private void writeLong(long value) throws IOException {
        for (int i = (LONG_SIZE - 1) * 8; i >= 0; i -= 8) {
            out.write((byte) (value >> i));
        }
    }

    @Override
    public void writeDouble(double value) throws IOException {
        out.write(DOUBLE_TYPE);
        writeLong(Double.doubleToRawLongBits(value));
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        byte valueToWrite = BOOLEAN_TYPE;
        if (value) {
            valueToWrite |= 0x01;
        }
        out.write(valueToWrite);
    }

    @Override
    public void writeString(String value) throws IOException {
        out.write(STRING_TYPE);
        short length = (short) value.length();
        out.write(length >> 8);
        out.write(length);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.write(bytes);
    }

    @Override
    public void writeRaw(byte[] value) throws IOException {
        out.write(RAW_TYPE);
        short length = (short) value.length;
        out.write(length >> 8);
        out.write(length);
        out.write(value);
    }

    @Override
    public void writeStartOfTheList() throws IOException {
        out.write(LIST_TYPE);
    }

    @Override
    public void writeEndOfTheList() throws IOException {
        out.write(NULL_TYPE);
    }
}
