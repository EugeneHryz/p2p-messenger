package com.eugene.wc.protocol.data;

import static com.eugene.wc.protocol.api.data.TypesDefinition.*;

import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.plugin.TransportId;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

public class StreamDataWriterImpl implements StreamDataWriter {

    // sizes are in bytes
    private static final int LONG_SIZE = DOUBLE_SIZE;

    private final OutputStream out;

    @Inject
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
    public void writeListStart() throws IOException {
        out.write(LIST_TYPE);
    }

    @Override
    public void writeListEnd() throws IOException {
        out.write(NULL_TYPE);
    }

    @Override
    public void writeWdfList(WdfList list) throws IOException {
        out.write(LIST_TYPE);
        writeWdfListRecursively(list, 0, 1);
    }

    private void writeWdfListRecursively(WdfList list, int index, int level) throws IOException {
        if (index < list.size()) {

            Object obj = list.get(index);
            if (obj instanceof WdfList) {
                writeStartOfTheList();
                WdfList nestedList = (WdfList) obj;
                writeWdfListRecursively(nestedList, 0, level + 1);
            } else {
                writeObject(obj);
            }
            writeWdfListRecursively(list, index + 1, level);
        } else if (index == list.size()) {
            writeEndOfTheList();
        }
    }

    private void writeObject(Object obj) throws IOException {
        if (obj instanceof Boolean) {
            Boolean value = (Boolean) obj;
            writeBoolean(value);
        } else if (obj instanceof Integer) {
            Integer value = (Integer) obj;
            writeInteger(value);
        } else if (obj instanceof Double) {
            Double value = (Double) obj;
            writeDouble(value);
        } else if (obj instanceof String) {
            String str = (String) obj;
            writeString(str);
        } else if (obj instanceof byte[]) {
            byte[] rawBytes = (byte[]) obj;
            writeRaw(rawBytes);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void writeStartOfTheList() throws IOException {
        out.write(LIST_TYPE);
    }

    private void writeEndOfTheList() throws IOException {
        out.write(NULL_TYPE);
    }
}
