package com.eugene.wc.protocol.contact.exchange;

import com.eugene.wc.protocol.api.contact.exchange.IdentityDecoder;
import com.eugene.wc.protocol.api.crypto.PublicKey;
import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.identity.Identity;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;
import com.eugene.wc.protocol.data.StreamDataReaderImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class IdentityDecoderImpl implements IdentityDecoder {

    @Override
    public Identity decode(byte[] encoded) throws DecodeException {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encoded)) {
            StreamDataReader dataReader = new StreamDataReaderImpl(bais);

            String identityName = dataReader.readNextString();
            byte[] publicKeyBytes = dataReader.readNextRaw();

            PublicKey publicKey = new PublicKey(publicKeyBytes);

            return new Identity(publicKey, identityName);

        } catch (IOException e) {
            throw new DecodeException("Unable to decode Identity");
        }
    }
}
