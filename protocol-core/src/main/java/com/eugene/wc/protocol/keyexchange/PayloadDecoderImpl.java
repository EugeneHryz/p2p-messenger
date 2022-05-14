package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadDecoder;
import com.eugene.wc.protocol.api.keyexchange.TransportDescriptor;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;
import com.eugene.wc.protocol.api.plugin.TransportId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

public class PayloadDecoderImpl implements PayloadDecoder {

    private static final Logger logger = Logger.getLogger(PayloadDecoderImpl.class.getName());

    private final StreamDataReader reader;

    @Inject
    public PayloadDecoderImpl(StreamDataReader reader) {
        this.reader = reader;
    }

    @Override
    public Payload decode() throws DecodeException {
        try {
            byte[] commitment = reader.readNextRaw();

            List<TransportDescriptor> descriptors = new ArrayList<>();
            WdfList list = reader.readNextWdfList();
            for (int i = 0; i < list.size(); i += 2) {
                TransportId transportId = new TransportId(list.getString(i));

                if (i + 1 < list.size()) {
                    WdfList properties = list.getWdfList(i + 1);

                    TransportDescriptor td = new TransportDescriptor(transportId, properties);
                    descriptors.add(td);
                }
            }
            return new Payload(commitment, descriptors);

        } catch (IOException e) {
            logger.severe("Unable to decode payload " + e);
            throw new DecodeException("Unable to decode payload ", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                logger.warning("Unable to close reader " + e);
            }
        }
    }
}
