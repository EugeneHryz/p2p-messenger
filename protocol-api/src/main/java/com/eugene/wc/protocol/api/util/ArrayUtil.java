package com.eugene.wc.protocol.api.util;

public class ArrayUtil {

    public static boolean compare(byte[] a1, int startPos1, byte[] a2, int startPos2, int length) {
        if (startPos1 >= a1.length || startPos2 >= a2.length || startPos1 < 0 || startPos2 < 0) {
            throw new IllegalArgumentException("startPos cannot be out of range of the array");
        }
        int i = 0;
        boolean equal = true;
        while (i < length && startPos1 + i < a1.length && startPos2 + i < a2.length && equal) {
            if (a1[startPos1 + i] != a2[startPos2 + i]) {
                equal = false;
            }
            i++;
        }
        return equal;
    }
}
