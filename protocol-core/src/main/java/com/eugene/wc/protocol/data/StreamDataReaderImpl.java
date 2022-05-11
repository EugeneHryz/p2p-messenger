package com.eugene.wc.protocol.data;

import static com.eugene.wc.protocol.api.data.TypesDefinition.*;

import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.data.exception.FormatException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

public class StreamDataReaderImpl implements StreamDataReader {

    private final InputStream in;

    private byte nextByte;
    private boolean readLookahead;

    @Inject
    public StreamDataReaderImpl(InputStream in) {
        this.in = in;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public int readNextInt() throws IOException {
        if (!hasInteger()) {
            throw new FormatException();
        }
        readLookahead = false;
        return readInteger32();
    }

    @Override
    public double readNextDouble() throws IOException {
        if (!hasDouble()) {
            throw new FormatException();
        }
        readLookahead = false;
        long value = 0;
        for (int i = (DOUBLE_SIZE - 1) * 8; i >= 0; i -= 8) {
            int nextByte = in.read();
            value |= ((long) nextByte << i);
        }
        return Double.longBitsToDouble(value);
    }

    @Override
    public String readNextString() throws IOException {
        if (!hasString()) {
            throw new FormatException();
        }
        readLookahead = false;
        short strLength = readInteger16();
        byte[] strBytes = in.readNBytes(strLength);

        String str = new String(strBytes, StandardCharsets.UTF_8);
        return str;
    }

    @Override
    public boolean readNextBoolean() throws IOException {
        if (!hasBoolean()) {
            throw new FormatException();
        }
        readLookahead = false;
        return (0x01 & nextByte) != 0;
    }

    @Override
    public byte[] readNextRaw() throws IOException {
        if (!hasRaw()) {
            throw new FormatException();
        }
        readLookahead = false;
        short length = readInteger16();
        return in.readNBytes(length);
    }

    @Override
    public WdfList readNextWdfList() throws IOException {
        if (!hasListStart()) {
            throw new FormatException();
        }
        readLookahead = false;
        WdfList list = new WdfList();
        readWdfListRecursively(list, 1);
        return list;
    }

    private WdfList readWdfListRecursively(WdfList list, int level) throws IOException {
        if (isEof()) {
            return list;
        }
        if (hasListStart()) {
            readLookahead = false;
            WdfList nestedList = new WdfList();
            list.add(nestedList);
            readWdfListRecursively(nestedList, level + 1);
            return readWdfListRecursively(list, level);
        } else if (hasEnd()) {
            readLookahead = false;
            return list;
        }
        Object obj = readNextObject();
        readLookahead = false;
        list.add(obj);
        return readWdfListRecursively(list, level);
    }

    private Object readNextObject() throws IOException {
        Object value;
        if (hasBoolean()) {
            value = readNextBoolean();
        } else if (hasInteger()) {
            value = readNextInt();
        } else if (hasDouble()) {
            value = readNextDouble();
        } else if (hasString()) {
            value = readNextString();
        } else if (hasRaw()) {
            value = readNextRaw();
        } else {
            throw new IllegalArgumentException();
        }
        return value;
    }

    @Override
    public boolean isEof() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == -1;
    }

    private boolean hasInteger() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == INTEGER_TYPE;
    }

    private boolean hasDouble() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == DOUBLE_TYPE;
    }

    private boolean hasString() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == STRING_TYPE;
    }

    private boolean hasBoolean() throws IOException {
        if (!readLookahead) readLookahead();
        byte justType = (byte) (nextByte & 0xF0);
        return justType == BOOLEAN_TYPE;
    }

    private boolean hasRaw() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == RAW_TYPE;
    }

    private boolean hasListStart() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == LIST_TYPE;
    }

    private boolean hasEnd() throws IOException {
        if (!readLookahead) readLookahead();
        return nextByte == NULL_TYPE;
    }

    private void readLookahead() throws IOException {
        nextByte = (byte) in.read();
        readLookahead = true;
    }

    private int readInteger32() throws IOException {
        int value = 0;
        for (int i = (INTEGER_SIZE - 1) * 8; i >= 0; i -= 8) {
            int nextByte = in.read();
            value |= (nextByte << i);
        }
        return value;
    }

    private short readInteger16() throws IOException {
        short value = (short) (in.read() << 8);
        value |= (short) in.read();
        return value;
    }
}
