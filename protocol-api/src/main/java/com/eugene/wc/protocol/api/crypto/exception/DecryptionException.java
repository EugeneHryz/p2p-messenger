package com.eugene.wc.protocol.api.crypto.exception;

public class DecryptionException extends CryptoException {

    public DecryptionException() {
        super();
    }

    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecryptionException(Throwable cause) {
        super(cause);
    }
}
