package com.eugene.wc.protocol.transport;

import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;

import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportReader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TransportReaderImpl implements TransportReader {

    private final DataInputStream input;

    public TransportReaderImpl(InputStream in) {
        if (!in.markSupported()) in = new BufferedInputStream(in, 1);
        input = new DataInputStream(in);
    }

    @Override
    public EncryptedPacket readNextPacket() throws IOException {
        byte[] tagBytes = new byte[TAG_LENGTH];
        input.readFully(tagBytes);

        int contentLength = readInteger32();
        byte[] content = new byte[contentLength];
        input.readFully(content);

        return new EncryptedPacket(new Tag(tagBytes), content);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    private int readInteger32() throws IOException {
        int value = 0;
        for (int i = 24; i >= 0; i -= 8) {
            int nextByte = input.read();
            value |= (nextByte << i);
        }
        return value;
    }
}
