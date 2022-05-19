package com.eugene.wc.protocol.keyexchange;

import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.keyexchange.Payload;
import com.eugene.wc.protocol.api.keyexchange.PayloadDecoder;
import com.eugene.wc.protocol.api.keyexchange.PayloadEncoder;
import com.eugene.wc.protocol.api.keyexchange.TransportDescriptor;
import com.eugene.wc.protocol.api.keyexchange.exception.DecodeException;
import com.eugene.wc.protocol.api.keyexchange.exception.EncodeException;
import com.eugene.wc.protocol.api.plugin.TransportId;
import com.eugene.wc.protocol.data.StreamDataReaderImpl;
import com.eugene.wc.protocol.data.StreamDataWriterImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class PayloadEncodeDecodeTest {

    private ByteArrayOutputStream baos;
    private ByteArrayInputStream bais;

    private PayloadEncoder encoder;
    private PayloadDecoder decoder;

    @Before
    public void setup() {
        baos = new ByteArrayOutputStream();
        StreamDataWriter dataWriter = new StreamDataWriterImpl(baos);
        encoder = new PayloadEncoderImpl(dataWriter);
    }

    @After
    public void cleanup() {
        try {
            baos.close();
            if (bais != null) {
                bais.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void payloadEncodeDecodeTest() throws EncodeException, DecodeException {
        byte[] commitment = {0x34, (byte) 0xA0, 0x09, 0x50, 0x33, (byte) 0xB7, 0x70, 0x13, 0x09, 0x08,
                0x00, 0x1C, 0x34, (byte) 0xA0, 0x09, 0x50, 0x39, (byte) 0xB7, 0x70, 0x03, 0x09, 0x18,
                0x56, 0x2F, 0x34, (byte) 0xAA, (byte) 0xFF, 0x50, 0x33, 0x07, 0x70, 0x13};
        WdfList props = new WdfList();
        props.add("this is a test");
        props.add(890);
        props.add("12--89jjj--11122");
        WdfList containingList = new WdfList();
        containingList.add("wowowowowow");
        containingList.add(props);

        TransportDescriptor td = new TransportDescriptor(new TransportId("com.example.app.bluetooth"),
                containingList);

        Payload payload = new Payload(commitment, Arrays.asList(td));
        encoder.encode(payload);

        byte[] bytes = baos.toByteArray();

        // initialize decoder
        bais = new ByteArrayInputStream(bytes);
        StreamDataReader dataReader = new StreamDataReaderImpl(bais);
        decoder = new PayloadDecoderImpl(dataReader);

        Payload actual = decoder.decode();
        WdfList wdfList = actual.getDescriptors().get(0).getProperties();
        System.out.println("1st item: " + wdfList.getString(0));
        WdfList list = wdfList.getWdfList(1);
        if (list != null) {
            System.out.println("2nd item is list");
            System.out.println("1st item: " + list.getString(0));
            System.out.println("2nd item: " + list.getInteger(1));
            System.out.println("3rd item: " + list.getString(2));
        }

        Assert.assertEquals(payload, actual);
    }
}
