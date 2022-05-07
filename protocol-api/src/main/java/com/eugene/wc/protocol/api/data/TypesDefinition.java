package com.eugene.wc.protocol.api.data;

public final class TypesDefinition {

    private TypesDefinition() {
    }

    public static final byte INTEGER_TYPE = 0x10;

    public static final byte BOOLEAN_TYPE = 0x20;

    public static final byte DOUBLE_TYPE = 0x30;

    public static final byte STRING_TYPE = 0x40;

    public static final byte RAW_TYPE = 0x50;

    public static final byte NULL_TYPE = 0x60;

    public static final byte LIST_TYPE = 0x00;

    public static final int INTEGER_SIZE = 4;
    public static final int DOUBLE_SIZE = 8;
    public static final int LENGTH_SIZE = 2;
}
