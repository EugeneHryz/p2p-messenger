package com.eugene.wc.protocol.db;

public class DatabaseTypes {

    private final String stringType;
    private final String hashType;
    private final String binaryType;
    private final String counterType;

    public DatabaseTypes(String stringType, String hashType, String binaryType,
                         String counterType) {
        this.stringType = stringType;
        this.hashType = hashType;
        this.binaryType = binaryType;
        this.counterType = counterType;
    }

    public String getStringType() {
        return stringType;
    }

    public String getHashType() {
        return hashType;
    }

    public String getBinaryType() {
        return binaryType;
    }

    public String getCounterType() {
        return counterType;
    }

    public String replaceTypes(String value) {
        String result = value.replace("_STRING", stringType)
                .replace("_HASH", hashType)
                .replace("_BINARY", binaryType)
                .replace("_COUNTER", counterType);

        return result;
    }
}
