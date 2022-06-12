package com.eugene.wc.protocol.db;

public class DatabaseTypes {

    private final String stringType;
    private final String hashType;
    private final String binaryType;
    private final String counterType;
    private final String secretType;

    public DatabaseTypes(String stringType, String hashType, String binaryType,
                         String counterType, String secretType) {
        this.stringType = stringType;
        this.hashType = hashType;
        this.binaryType = binaryType;
        this.counterType = counterType;
        this.secretType = secretType;
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

    public String getSecretType() {
        return secretType;
    }

    public String replaceTypes(String value) {
        String result = value.replace("_STRING", stringType)
                .replace("_HASH", hashType)
                .replace("_BINARY", binaryType)
                .replace("_COUNTER", counterType)
                .replace("_SECRET", secretType);

        return result;
    }
}
