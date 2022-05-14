package com.eugene.wc.protocol.api.crypto;

import com.eugene.wc.protocol.api.ByteArray;

public class PrivateKey extends ByteArray {

//    public static final int PRIVATE_KEY_SIZE = 32;

    public PrivateKey(byte[] bytes) {
        super(bytes);
//        if (bytes.length != PRIVATE_KEY_SIZE) {
//            throw new IllegalArgumentException();
//        }
    }
}
