package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.ByteArray;

public class PublicKey extends ByteArray {

    public static final int PUBLIC_KEY_SIZE = 32;

    public PublicKey(byte[] bytes) {
        super(bytes);
        if (bytes.length != PUBLIC_KEY_SIZE) {
            throw new IllegalArgumentException("Illegal public key length");
        }
    }
}
