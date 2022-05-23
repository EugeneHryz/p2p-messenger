package com.eugene.wc.protocol.api.transport;

import static com.eugene.wc.protocol.api.transport.TransportConstants.TAG_LENGTH;

public class EncryptedPacket {

    private final Tag tag;
    private final byte[] content;

    public EncryptedPacket(Tag tag, byte[] content) {
        if (tag.getBytes().length != TAG_LENGTH) {
            throw new IllegalArgumentException("Illegal tag length");
        }
        this.tag = tag;
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    public Tag getTag() {
        return tag;
    }
}
