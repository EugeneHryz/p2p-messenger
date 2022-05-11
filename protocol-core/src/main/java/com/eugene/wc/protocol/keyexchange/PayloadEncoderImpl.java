package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadEncoder;
import com.eugene.wc.protocol.api.keyexchange.TransportDescriptor;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

public class PayloadEncoderImpl implements PayloadEncoder {

    private static final Logger logger = Logger.getLogger(PayloadEncoderImpl.class.getName());

    private final StreamDataWriter writer;

    @Inject
    public PayloadEncoderImpl(StreamDataWriter writer) {
        this.writer = writer;
    }

    @Override
    public void encode(Payload payload) throws EncodeException {
        try {
            writer.writeRaw(payload.getCommitment());

            List<TransportDescriptor> descriptors = payload.getDescriptors();
            writer.writeListStart();
            for (TransportDescriptor td : descriptors) {
                String transportId = td.getTransportId();
                writer.writeString(transportId);

                WdfList transportProps = td.getProperties();
                writer.writeWdfList(transportProps);
            }
            writer.writeListEnd();
        } catch (IOException e) {
            logger.severe("Unable to encode payload " + e);
            throw new EncodeException("Unable to encode payload", e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                logger.warning("Unable to close writer " + e);
            }
        }
    }
}
