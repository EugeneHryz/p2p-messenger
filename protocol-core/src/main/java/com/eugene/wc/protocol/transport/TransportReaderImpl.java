package com.eugene.wc.protocol.transport;

import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;

import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.Tag;
import com.eugene.wc.protocol.api.transport.TransportReader;
import com.eugene.wc.protocol.api.util.ByteUtils;

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

        byte[] lengthBytes = new byte[ByteUtils.INT_32_BYTES];
        input.readFully(lengthBytes);
        int contentLength = (int) ByteUtils.readUint32(lengthBytes, 0);
        byte[] content = new byte[contentLength];
        input.readFully(content);

        return new EncryptedPacket(new Tag(tagBytes), content);
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}
