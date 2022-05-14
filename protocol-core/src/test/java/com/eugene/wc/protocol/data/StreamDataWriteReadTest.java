package com.eugene.wc.protocol.data;

import com.eugene.wc.protocol.api.data.StreamDataReader;
import com.eugene.wc.protocol.api.data.StreamDataWriter;
import com.eugene.wc.protocol.api.data.WdfList;
import com.eugene.wc.protocol.api.plugin.TransportId;

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

    @Test
    public void testSimpleListWriteRead() throws IOException {
        WdfList list = new WdfList();
        list.add(904);
        list.add("How was your day?o_ooo");
        list.add(43.8);
        list.add(false);
        list.add(true);

        writer.writeWdfList(list);
        writer.flush();

        WdfList actual = reader.readNextWdfList();
        Assert.assertEquals(list, actual);
    }

    @Test
    public void testNestedListWriteRead() throws IOException {
        WdfList mainList = new WdfList();
        mainList.add(904);
        mainList.add("How was your day?o_ooo");
        mainList.add(-9000.123);
        mainList.add(false);
        mainList.add(true);

        WdfList nestedList = new WdfList();
        nestedList.add("189:jsjsjsjs(9001111");

        WdfList anotherNestedList = new WdfList();
        anotherNestedList.add(12);
        anotherNestedList.add(780);
        anotherNestedList.add("jkjad dsnsns 111");

        nestedList.add(anotherNestedList);
        nestedList.add("wooooooosh    l");

        mainList.add(nestedList);
        mainList.add(112.0006);
        mainList.add(anotherNestedList);

        writer.writeWdfList(mainList);
        writer.flush();

        WdfList actual = reader.readNextWdfList();
        Assert.assertEquals(mainList, actual);
    }
}
