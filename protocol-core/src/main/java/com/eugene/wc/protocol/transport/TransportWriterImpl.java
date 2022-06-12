package com.eugene.wc.protocol.transport;

import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.TransportWriter;
import com.eugene.wc.protocol.api.util.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;

public class TransportWriterImpl implements TransportWriter {

    private final OutputStream output;

    public TransportWriterImpl(OutputStream output) {
        this.output = output;
    }

    @Override
    public void writePacket(EncryptedPacket packet) throws IOException {
        output.write(packet.getTag().getBytes());

        byte[] content = packet.getContent();
        byte[] contentLength = new byte[ByteUtils.INT_32_BYTES];
        ByteUtils.writeUint32(content.length, contentLength, 0);
        output.write(contentLength);
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
