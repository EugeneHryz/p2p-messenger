package com.eugene.wc.protocol.api.session;

public final class MessageTypes {

    public static final byte MESSAGE = 0x01;
    public static final byte ACK = 0x02;
    public static final byte END_OF_SESSION = (byte) 0xFF;

    private MessageTypes() {
    }
}
