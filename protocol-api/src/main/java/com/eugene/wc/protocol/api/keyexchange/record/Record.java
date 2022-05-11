package com.eugene.wc.protocol.api.keyexchange.record;

public class Record {

    public static class Type {
        public static final byte KEY = 0x01;
        public static final byte CONFIRM = 0x02;
        public static final byte ABORT = 0x03;
    }

    private final byte type;
    private final byte[] content;

    public Record(byte type) {
        this.type = type;
        this.content = new byte[0];
    }

    public Record(byte type, byte[] content) {
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
