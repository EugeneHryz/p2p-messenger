package com.eugene.wc.protocol.api.util;

import static java.nio.charset.CodingErrorAction.IGNORE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

import org.apache.commons.lang3.ArrayUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collection;
import java.util.regex.Pattern;


public class StringUtils {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static Pattern MAC = Pattern.compile("[0-9a-f]{2}:[0-9a-f]{2}:" +
                    "[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}",
            CASE_INSENSITIVE);

    private static final char[] HEX = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static String join(Collection<String> strings, String separator) {
        StringBuilder joined = new StringBuilder();
        for (String s : strings) {
            if (joined.length() > 0) joined.append(separator);
            joined.append(s);
        }
        return joined.toString();
    }

    public static byte[] toUtf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean utf8IsTooLong(String s, int maxLength) {
        return toUtf8(s).length > maxLength;
    }

    public static String fromUtf8(byte[] bytes) {
        return fromUtf8(bytes, 0, bytes.length);
    }

    public static String fromUtf8(byte[] bytes, int off, int len) {
        CharsetDecoder decoder = UTF_8.newDecoder();
        decoder.onMalformedInput(IGNORE);
        decoder.onUnmappableCharacter(IGNORE);
        ByteBuffer buffer = ByteBuffer.wrap(bytes, off, len);
        try {
            return decoder.decode(buffer).toString();
        } catch (CharacterCodingException e) {
            throw new AssertionError(e);
        }
    }

    public static String truncateUtf8(String s, int maxUtf8Length) {
        byte[] utf8 = toUtf8(s);
        if (utf8.length <= maxUtf8Length) return s;
        return fromUtf8(utf8, 0, maxUtf8Length);
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

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

    public static boolean isValidMac(String mac) {
        return MAC.matcher(mac).matches();
    }
}
