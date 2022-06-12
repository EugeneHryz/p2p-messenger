package com.eugene.wc.protocol.api.session;

public class MessagePacket {
    
    private final byte type;
    private final byte[] content;

    public MessagePacket(byte type, byte[] content) {
        this.type = type;
        this.content = content;
    }

    public byte getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }
}
