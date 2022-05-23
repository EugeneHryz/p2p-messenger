package com.eugene.wc.protocol.contact.exchange;

import com.eugene.wc.protocol.api.contact.exchange.IdentityEncoder;
import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;
import com.eugene.wc.protocol.data.StreamDataWriterImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class IdentityEncoderImpl implements IdentityEncoder {

    @Override
    public byte[] encode(Identity identity) throws EncodeException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            StreamDataWriter dataWriter = new StreamDataWriterImpl(baos);

            dataWriter.writeString(identity.getName());
            dataWriter.writeRaw(identity.getPublicKey().getBytes());
            dataWriter.flush();

            return baos.toByteArray();

        } catch (IOException e) {
            throw new EncodeException("Unable to encode identity", e);
        }
    }
}
