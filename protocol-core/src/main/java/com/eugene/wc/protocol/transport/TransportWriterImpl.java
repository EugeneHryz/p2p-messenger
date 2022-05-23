package com.eugene.wc.protocol.transport;

import com.eugene.wc.protocol.api.transport.EncryptedPacket;
import com.eugene.wc.protocol.api.transport.TransportWriter;

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
        writeInteger32(content.length);
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

    private void writeInteger32(int value) throws IOException {
        for (int i = 24; i >= 0; i -= 8) {
            output.write(value >> i);
        }
    }
}
