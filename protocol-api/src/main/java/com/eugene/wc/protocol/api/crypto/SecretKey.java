package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.ByteArray;

public class SecretKey extends ByteArray {

    // secret key size in bytes
    public static final int SECRET_KEY_SIZE = 32;

    public SecretKey(byte[] bytes) {
        super(bytes);
        if (bytes.length != 32)
            throw new IllegalArgumentException("Secret key must be 32 bytes long");
    }
}
