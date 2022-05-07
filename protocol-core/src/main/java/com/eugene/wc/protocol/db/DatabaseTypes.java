package com.eugene.wc.protocol.db;

public class DatabaseTypes {

    private final String stringType;
    private final String hashType;

    public DatabaseTypes(String stringType, String hashType) {
        this.stringType = stringType;
        this.hashType = hashType;
    }

    public String getStringType() {
        return stringType;
    }

    public String getHashType() {
        return hashType;
    }

    public String replaceTypes(String value) {
        String result = value.replace("_STRING", stringType)
                .replace("_HASH", hashType);

        return result;
    }
}
