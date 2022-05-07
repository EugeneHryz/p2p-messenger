package com.eugene.wc.protocol.api.util;

import org.apache.commons.lang3.ArrayUtils;


public class StringUtils {

    private static final char[] HEX = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static char[] toHexChars(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {

            hex[j++] = HEX[(bytes[i] >> 4) & 0xF];
            hex[j++] = HEX[bytes[i] & 0xF];
        }
        return hex;
    }

    private static byte[] fromHexChars(char[] chars) {
        byte[] bytes = new byte[chars.length / 2];
        for (int i = 0, j = 0; i < bytes.length; i++, j += 2) {

            int rightPart = ArrayUtils.indexOf(HEX, chars[j]);
            byte result = (byte) (rightPart << 4 & 0xF0);

            int leftPart = ArrayUtils.indexOf(HEX, chars[j + 1]);
            result |= leftPart & 0x0F;
            bytes[i] = result;
        }
        return bytes;
    }

    public static String toHexString(byte[] array) {
        return new String(toHexChars(array));
    }

    public static byte[] fromHexString(String hex) {
        return fromHexChars(hex.toCharArray());
    }
}
