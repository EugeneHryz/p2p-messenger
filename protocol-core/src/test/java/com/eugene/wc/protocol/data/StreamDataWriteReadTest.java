package com.eugene.wc.protocol.data;

import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.StreamDataWriter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class StreamDataWriteReadTest {

    File file = new File("wdf_test.bin");
    StreamDataWriter writer;
    StreamDataReader reader;

    @Before
    public void setup() throws FileNotFoundException {

        FileOutputStream fileOut = new FileOutputStream(file);
        FileInputStream fileIn = new FileInputStream(file);

        writer = new StreamDataWriterImpl(fileOut);
        reader = new StreamDataReaderImpl(fileIn);
    }

    @After
    public void cleanup() throws IOException {
        reader.close();
        writer.close();
    }

    @Test
    public void testIntegerWriteRead() throws IOException {
        int intValue = 189;
        writer.writeInteger(intValue);
        writer.flush();

        int readInt = reader.readNextInt();
        Assert.assertEquals(intValue, readInt);
    }

    @Test
    public void testDoubleWriteRead() throws IOException {
        double doubleValue = -164.90;
        writer.writeDouble(doubleValue);
        writer.flush();

        double readDouble = reader.readNextDouble();
        Assert.assertEquals(doubleValue, readDouble, 0.0001);
    }

    @Test
    public void testStringWriteRead() throws IOException {
        String str = "Hello!!!100;a klq";
        writer.writeString(str);
        writer.flush();

        String readStr = reader.readNextString();
        Assert.assertEquals(str, readStr);
    }

    @Test
    public void testBooleanWriteRead() throws IOException {
        boolean value = false;
        writer.writeBoolean(value);
        writer.flush();

        boolean readValue = reader.readNextBoolean();
        Assert.assertFalse(readValue);
    }
}
