package com.eugene.wc.protocol.api.transport;

import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;

import com.eugene.wc.protocol.api.ByteArray;

public class Tag extends ByteArray {

    public Tag(byte[] bytes) {
        super(bytes);
        if (bytes.length != TAG_LENGTH) {
            throw new IllegalArgumentException("Illegal tag length");
        }
    }
}
