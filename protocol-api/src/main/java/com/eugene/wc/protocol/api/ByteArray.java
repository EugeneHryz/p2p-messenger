package com.eugene.wc.protocol.api;

import java.util.Arrays;

public class ByteArray implements Comparable<ByteArray> {

    private final byte[] bytes;

    public ByteArray(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteArray byteArray = (ByteArray) o;
        return Arrays.equals(bytes, byteArray.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public int compareTo(ByteArray other) {
        return compare(bytes, other.bytes);
    }

    public static int compare(byte[] a, byte[] b) {
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int aUnsigned = a[i] & 0xFF, bUnsigned = b[i] & 0xFF;
            if (aUnsigned < bUnsigned) return -1;
            if (aUnsigned > bUnsigned) return 1;
        }
        return a.length - b.length;
    }
}
