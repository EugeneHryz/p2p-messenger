package com.eugene.wc.protocol.api.util;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testConversionFromByteArrayToHexString() {
        byte[] input = { 0x50, 0x78, (byte) 0xC3, 0x21, 0x0A, (byte) 0xFF };
        String expected = "5078C3210AFF";

        String actual = StringUtils.toHexString(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testConversionFromHexStringToByteArray() {
        String input = "13F8D0AA0B5390";
        byte[] expected = { 0x13, (byte) 0xF8, (byte) 0xD0, (byte) 0xAA, 0x0B, 0x53, (byte) 0x90 };

        byte[] actual = StringUtils.fromHexString(input);
        Assert.assertArrayEquals(expected, actual);
    }
}
